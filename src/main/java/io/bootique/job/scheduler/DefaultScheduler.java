package io.bootique.job.scheduler;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobParameterMetadata;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.SingleJob;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJob;
import io.bootique.job.runnable.RunnableJobFactory;
import io.bootique.job.scheduler.execution.ExecutionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import java.util.Collection;
import java.util.Collections;
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
	private Collection<Job> jobGroups;
	private Collection<TriggerDescriptor> triggers;
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
		this.jobDefinitions = jobDefinitions;

		this.jobs = jobs;
		this.jobGroups = collectJobGroups(jobDefinitions, executionFactory);
	}

	private Collection<Job> collectJobGroups(Map<String, JobDefinition> jobDefinitions,
											 ExecutionFactory executionFactory) {
		return jobDefinitions.entrySet().stream()
				// filter out standalone jobs without dependencies
				.filter(e -> !(e.getValue() instanceof SingleJob) || (((SingleJob) e.getValue()).getDependsOn().isPresent()))
				.map(e -> createJobGroup(e.getKey(), executionFactory))
				.collect(Collectors.toList());
	}

	private Job createJobGroup(String name, ExecutionFactory executionFactory) {
		return new LazyJobGroup(name, executionFactory, this);
	}

	@Override
	public JobFuture runOnce(String jobName) {
		return runOnce(jobName, Collections.emptyMap());
	}

	@Override
	public JobFuture runOnce(String jobName, Map<String, Object> parameters) {
		if (findJobByName(jobName, mapJobGroups()).isPresent()) {
			return runJobGroup(jobName, parameters);
		} else {
			return runStandaloneJob(jobName, parameters);
		}
	}

	JobFuture runJobGroup(String groupName, Map<String, Object> parameters) {
		Optional<Job> jobOptional = findJobByName(groupName, mapJobGroups());
		return jobOptional.isPresent() ? runJobGroupWithParameters(jobOptional.get(), parameters) : invalidJobNameResult(groupName);
	}

	JobFuture runStandaloneJob(String jobName, Map<String, Object> parameters) {
		Optional<Job> jobOptional = findJobByName(jobName, mapJobs());
		return jobOptional.isPresent() ? runJobWithParameters(jobOptional.get(), parameters) : invalidJobNameResult(jobName);
	}

	private Optional<Job> findJobByName(String jobName, Map<String, Job> jobs) {
		Job job = jobs.get(jobName);
		return (job == null) ? Optional.empty() : Optional.of(job);
	}

	private Map<String, Job> mapJobs() {
		return jobs.stream().collect(toMap(j -> j.getMetadata().getName(), j -> j));
	}

	private Map<String, Job> mapJobGroups() {
		return jobGroups.stream().collect(toMap(j -> j.getMetadata().getName(), j -> j));
	}

	private JobFuture invalidJobNameResult(String jobName) {
		return new JobFuture(new ExpiredFuture(),
					() -> JobResult.failure(JobMetadata.build(jobName), "Invalid job name: " + jobName));
	}

	private JobFuture runJobGroupWithParameters(Job group, Map<String, Object> parameters) {
		Map<String, Object> mergedParameters = mergeParams(parameters, jobParams(group));

		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = taskScheduler.schedule(() -> result[0] = group.run(mergedParameters), new Date());
		return new JobFuture(jobFuture, () -> result[0] != null ? result[0] : JobResult.unknown(group.getMetadata()));
	}

	private JobFuture runJobWithParameters(Job job, Map<String, Object> parameters) {
		parameters = mergeParams(parameters, jobParams(job));

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
