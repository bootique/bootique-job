package io.bootique.job.scheduler;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobParameterMetadata;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.JobGroup;
import io.bootique.job.config.SingleJob;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJob;
import io.bootique.job.runnable.RunnableJobFactory;
import io.bootique.job.scheduler.execution.ExecutionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class DefaultScheduler implements Scheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

	private TaskScheduler taskScheduler;
	private RunnableJobFactory runnableJobFactory;
	private Collection<Job> jobs;
	private Collection<TriggerDescriptor> triggers;
	private ExecutionFactory executionFactory;
	private Map<String, JobDefinition> jobDefinitions;

	public DefaultScheduler(Collection<Job> jobs,
							Collection<TriggerDescriptor> triggers,
							TaskScheduler taskScheduler,
							RunnableJobFactory runnableJobFactory,
							ExecutionFactory executionFactory,
							Map<String, JobDefinition> jobDefinitions) {

		this.triggers = triggers;
		this.taskScheduler = taskScheduler;
		this.runnableJobFactory = runnableJobFactory;
		this.executionFactory = executionFactory;
		this.jobDefinitions = jobDefinitions;

		this.jobs = collectFlattenedJobs(jobs);
	}

	private Collection<Job> collectFlattenedJobs(Collection<Job> jobs) {
		List<Job> allJobs = new ArrayList<>(jobs);
		allJobs.addAll(collectJobGroups(jobDefinitions, executionFactory));
		return allJobs;
	}

	private Collection<Job> collectJobGroups(Map<String, JobDefinition> jobDefinitions,
											 ExecutionFactory executionFactory) {
		return jobDefinitions.entrySet().stream()
				.filter(e -> e.getValue() instanceof JobGroup)
				.collect(ArrayList::new,
						(acc, e) -> acc.add(createJobGroup(e.getKey(), executionFactory)),
						ArrayList::addAll);
	}

	private Job createJobGroup(String name, ExecutionFactory executionFactory) {
		return new LazyJobGroup(name, executionFactory, this);
	}

	private Map<String, Job> mapJobs() {
		return jobs.stream().collect(toMap(j -> j.getMetadata().getName(), j -> j));
	}

	@Override
	public JobFuture runOnce(String jobName) {
		Optional<Job> jobOptional = findJobByName(jobName);
		return jobOptional.isPresent() ? runJob(jobOptional.get()) : invalidJobNameResult(jobName);
	}

	@Override
	public JobFuture runOnce(String jobName, Map<String, Object> parameters) {
		Optional<Job> jobOptional = findJobByName(jobName);

		Job job = jobOptional.get();
		parameters = mergeParams(parameters, jobParams(job));
		return jobOptional.isPresent() ? runJobWithParameters(job, parameters) : invalidJobNameResult(jobName);
	}

	private Optional<Job> findJobByName(String jobName) {
		Map<String, Job> jobs = mapJobs();
		Job job = jobs.get(jobName);
		return (job == null) ? Optional.empty() : Optional.of(job);
	}

	private JobFuture invalidJobNameResult(String jobName) {
		return new JobFuture(new ExpiredFuture(),
					() -> JobResult.failure(JobMetadata.build(jobName), "Invalid job name: " + jobName));
	}

	private JobFuture runJob(Job job) {
		return runJobWithParameters(job, jobParams(job));
	}

	private JobFuture runJobWithParameters(Job job, Map<String, Object> parameters) {
		RunnableJob rj = runnableJobFactory.runnable(job, parameters);

		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = taskScheduler.schedule(() -> result[0] = rj.run(), new Date());
		return new JobFuture(jobFuture, () -> result[0] != null ? result[0] : JobResult.unknown(job.getMetadata()));
	}

	@Override
	public int start() {

		if (triggers.isEmpty()) {
			LOGGER.info("No triggers, exiting");
			return 0;
		}

		Map<String, Job> jobs = mapJobs();

		List<String> badTriggers = triggers.stream().filter(t -> !jobs.containsKey(t.getJob()))
				.map(t -> t.getJob() + ":" + t.getTrigger()).collect(Collectors.toList());

		if (badTriggers.size() > 0) {
			throw new IllegalStateException("Jobs are not found for the following triggers: " + badTriggers);
		}

		triggers.stream().forEach(tc -> {

			Job job = jobs.get(tc.getJob());
			Map<String, Object> parameters = jobParams(job);

			LOGGER.info(String.format("Will schedule '%s'.. (%s)", job.getMetadata().getName(), tc.describeTrigger()));

			RunnableJob rj = runnableJobFactory.runnable(job, parameters);
			taskScheduler.schedule(() -> rj.run(), tc.createTrigger());
		});

		return triggers.size();
	}

	protected Map<String, Object> jobParams(Job job) {

		// Taking parameters from Environment (i.e. System, etc.). Meaning
		// parameters can be passed as -Dp1=v1 -Dp2=v2 .. also can be bound in
		// YAML.

		// Property name is built using the following namespace convention:
		// <prefix>.jobname.paraname ("prefix" is usually "jobs").

		Map<String, Object> params = new HashMap<>();
		for (JobParameterMetadata<?> param : job.getMetadata().getParameters()) {
			String valueString = propertyValue(job.getMetadata(), param);
			Object value = param.fromString(valueString);
			params.put(param.getName(), value);
		}

		return params;
	}

	protected Map<String, Object> mergeParams(Map<String, Object> overridenParams, Map<String, Object> defaultParams) {
		Map<String, Object> merged = new HashMap<>(defaultParams);
		merged.putAll(overridenParams);
		return merged;
	}

	private String propertyValue(JobMetadata jobMD, JobParameterMetadata<?> param) {
		JobDefinition jobDefinition = jobDefinitions.get(jobMD.getName());
		if (jobDefinition instanceof SingleJob) {
			Object paramObj = ((SingleJob) jobDefinition).getParams().get(param.getName());
			if (paramObj != null) {
				return paramObj.toString();
			}
		}
		return null;
	}
}
