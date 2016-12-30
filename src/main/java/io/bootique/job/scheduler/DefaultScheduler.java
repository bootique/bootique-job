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
import io.bootique.job.scheduler.execution.Execution;
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
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

public class DefaultScheduler implements Scheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

	private TaskScheduler taskScheduler;
	private RunnableJobFactory runnableJobFactory;
	private ExecutionFactory executionFactory;
	private Set<String> availableJobs;
	private Collection<TriggerDescriptor> triggers;
	private Map<String, JobDefinition> jobDefinitions;

	public DefaultScheduler(Collection<Job> jobs,
							Collection<TriggerDescriptor> triggers,
							TaskScheduler taskScheduler,
							RunnableJobFactory runnableJobFactory,
							Map<String, JobDefinition> jobDefinitions) {

		this.triggers = triggers;
		this.runnableJobFactory = runnableJobFactory;
		this.taskScheduler = taskScheduler;
		this.availableJobs = collectJobNames(jobs, jobDefinitions);
		this.jobDefinitions = jobDefinitions;
		this.executionFactory = new ExecutionFactory(jobs, jobDefinitions, this);
	}

	private Set<String> collectJobNames(Collection<Job> jobs, Map<String, JobDefinition> jobDefinitions) {
		Set<String> jobNames = jobs.stream().map(job -> job.getMetadata().getName()).collect(Collectors.toSet());
		jobNames.addAll(jobDefinitions.keySet());
		return jobNames;
	}

	@Override
	public int start() {

		if (triggers.isEmpty()) {
			LOGGER.info("No triggers, exiting");
			return 0;
		}

		List<String> badTriggers = triggers.stream().filter(t -> !availableJobs.contains(t.getJob()))
				.map(t -> t.getJob() + ":" + t.getTrigger()).collect(Collectors.toList());

		if (badTriggers.size() > 0) {
			throw new IllegalStateException("Jobs are not found for the following triggers: " + badTriggers);
		}

		triggers.stream().forEach(tc -> {

			String jobName = tc.getJob();
			LOGGER.info(String.format("Will schedule '%s'.. (%s)", jobName, tc.describeTrigger()));

			Execution job = executionFactory.getExecution(tc.getJob());
			Map<String, Object> parameters = jobParams(job);

			schedule(job, parameters, job.getMetadata(), tc.createTrigger());
		});

		return triggers.size();
	}

	@Override
	public JobFuture runOnce(String jobName) {
		return runOnce(jobName, Collections.emptyMap());
	}

	@Override
	public JobFuture runOnce(String jobName, Map<String, Object> parameters) {
		Optional<Execution> jobOptional = findJobByName(jobName);
		if (jobOptional.isPresent()) {
			Execution job = jobOptional.get();
			parameters = mergeParams(parameters, jobParams(job));
			return runOnce(job, parameters, job.getMetadata(), new Date());
		} else {
			return invalidJobNameResult(jobName);
		}
	}

	private Optional<Execution> findJobByName(String jobName) {
		Execution job = executionFactory.getExecution(jobName);
		return (job == null) ? Optional.empty() : Optional.of(job);
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

	public JobFuture runOnce(Job job, Map<String, Object> parameters, JobMetadata metadata, Date date) {
		RunnableJob rj = runnableJobFactory.runnable(job, parameters);
		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = taskScheduler.schedule(() -> result[0] = rj.run(), date);
		return new JobFuture(jobFuture, () -> result[0] != null ? result[0] : JobResult.unknown(metadata));
	}

	public ScheduledFuture<?> schedule(Job job, Map<String, Object> parameters, JobMetadata metadata, Trigger trigger) {
		RunnableJob rj = runnableJobFactory.runnable(job, parameters);
		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = taskScheduler.schedule(() -> result[0] = rj.run(), trigger);
		return new JobFuture(jobFuture, () -> result[0] != null ? result[0] : JobResult.unknown(metadata));
	}
}
