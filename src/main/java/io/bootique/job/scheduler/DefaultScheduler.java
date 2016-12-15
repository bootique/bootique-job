package io.bootique.job.scheduler;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobParameterMetadata;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.SingleJob;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJobFactory;
import io.bootique.job.scheduler.execution.ExecutionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class DefaultScheduler implements Scheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

	private TaskScheduler taskScheduler;
	private Collection<Job> jobs;
	private Collection<ScheduledJob> jobGroups;
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
		this.jobDefinitions = jobDefinitions;

		this.jobs = jobs;
		this.jobGroups = collectJobGroups(jobs, jobDefinitions, executionFactory, runnableJobFactory);
	}

	private Collection<ScheduledJob> collectJobGroups(Collection<Job> jobs,
													  Map<String, JobDefinition> jobDefinitions,
													  ExecutionFactory executionFactory,
													  RunnableJobFactory runnableJobFactory) {

		Map<String, Job> jobMap = jobs.stream().collect(toMap(j -> j.getMetadata().getName(), j -> j));

		return jobDefinitions.entrySet().stream()
				.map(e -> {
					// don't create groups for standalone jobs without dependencies
					if (isStandaloneJob(e.getValue())) {
						return new ScheduledJob(jobMap.get(e.getKey()), runnableJobFactory, this);
					} else {
						return new ScheduledJob(createJobGroup(e.getKey(), executionFactory), runnableJobFactory, this);
					}
				})
				.collect(Collectors.toList());
	}

	private boolean isStandaloneJob(JobDefinition jobDefinition) {
		return jobDefinition instanceof SingleJob
				&& ((SingleJob) jobDefinition).getDependsOn().isPresent()
				&& ((SingleJob) jobDefinition).getDependsOn().get().isEmpty();
	}

	private Job createJobGroup(String name, ExecutionFactory executionFactory) {
		return new LazyJobGroup(name, executionFactory, this);
	}

	@Override
	public int start() {

		if (triggers.isEmpty()) {
			LOGGER.info("No triggers, exiting");
			return 0;
		}

		Map<String, ScheduledJob> jobs = mapJobLaunchers();

		List<String> badTriggers = triggers.stream().filter(t -> !jobs.containsKey(t.getJob()))
				.map(t -> t.getJob() + ":" + t.getTrigger()).collect(Collectors.toList());

		if (badTriggers.size() > 0) {
			throw new IllegalStateException("Jobs are not found for the following triggers: " + badTriggers);
		}

		triggers.stream().forEach(tc -> {

			String jobName = tc.getJob();
			LOGGER.info(String.format("Will schedule '%s'.. (%s)", jobName, tc.describeTrigger()));

			ScheduledJob job = jobs.get(tc.getJob());
			Map<String, Object> parameters = jobParams(job);

			job.schedule(parameters, tc.createTrigger());
		});

		return triggers.size();
	}

	@Override
	public JobFuture runOnce(String jobName) {
		return runOnce(jobName, Collections.emptyMap());
	}

	@Override
	public JobFuture runOnce(String jobName, Map<String, Object> parameters) {
		Optional<ScheduledJob> jobOptional = findJobByName(jobName, mapJobLaunchers());
		if (jobOptional.isPresent()) {
			ScheduledJob job = jobOptional.get();
			parameters = mergeParams(parameters, jobParams(job));
			return job.runAsync(parameters);
		} else {
			return invalidJobNameResult(jobName);
		}
	}

	public JobFuture runStandaloneJob(String jobName, Map<String, Object> parameters) {
		Optional<Job> jobOptional = findJobByName(jobName, mapJobs());
		if (jobOptional.isPresent()) {
			Job job = jobOptional.get();
			return runOnce(() -> job.run(mergeParams(parameters, jobParams(job))), job.getMetadata(), new Date());
		} else {
			return invalidJobNameResult(jobName);
		}
	}

	private <T extends Job> Optional<T> findJobByName(String jobName, Map<String, T> jobMap) {
		T job = jobMap.get(jobName);
		return (job == null) ? Optional.empty() : Optional.of(job);
	}

	private Map<String, ScheduledJob> mapJobLaunchers() {
		return jobGroups.stream().collect(toMap(j -> j.getMetadata().getName(), j -> j));
	}

	private Map<String, Job> mapJobs() {
		return jobs.stream().collect(toMap(j -> j.getMetadata().getName(), j -> j));
	}

	private JobFuture invalidJobNameResult(String jobName) {
		return new JobFuture(new ExpiredFuture(),
					() -> JobResult.failure(JobMetadata.build(jobName), "Invalid job name: " + jobName));
	}

	protected <T extends Job> Map<String, Object> jobParams(T job) {

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

	JobFuture runOnce(Supplier<JobResult> r, JobMetadata metadata, Date date) {
		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = taskScheduler.schedule(() -> result[0] = r.get(), date);
		return new JobFuture(jobFuture, () -> result[0] != null ? result[0] : JobResult.unknown(metadata));
	}

	ScheduledFuture<?> schedule(Supplier<JobResult> r, JobMetadata metadata, Trigger trigger) {
		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = taskScheduler.schedule(() -> result[0] = r.get(), trigger);
		return new JobFuture(jobFuture, () -> result[0] != null ? result[0] : JobResult.unknown(metadata));
	}
}
