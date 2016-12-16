package io.bootique.job.scheduler;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

public class DefaultScheduler implements Scheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

	private TaskScheduler taskScheduler;
	private RunnableJobFactory runnableJobFactory;
	private ExecutionFactory executionFactory;
	private Collection<TriggerDescriptor> triggers;

	public DefaultScheduler(Collection<TriggerDescriptor> triggers,
							TaskScheduler taskScheduler,
							RunnableJobFactory runnableJobFactory,
							ExecutionFactory executionFactory) {
		this.triggers = triggers;
		this.runnableJobFactory = runnableJobFactory;
		this.taskScheduler = taskScheduler;
		this.executionFactory = executionFactory;
	}

	@Override
	public int start() {

		if (triggers.isEmpty()) {
			LOGGER.info("No triggers, exiting");
			return 0;
		}

		List<String> badTriggers = triggers.stream().filter(t -> !executionFactory.getAvailableJobs().contains(t.getJob()))
				.map(t -> t.getJob() + ":" + t.getTrigger()).collect(Collectors.toList());

		if (badTriggers.size() > 0) {
			throw new IllegalStateException("Jobs are not found for the following triggers: " + badTriggers);
		}

		triggers.stream().forEach(tc -> {

			String jobName = tc.getJob();
			LOGGER.info(String.format("Will schedule '%s'.. (%s)", jobName, tc.describeTrigger()));

			Execution job = executionFactory.getExecution(tc.getJob());

			schedule(job, Collections.emptyMap(), tc.createTrigger());
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
			return runOnce(job, parameters, new Date());
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

	@Override
	public JobFuture runOnce(Job job, Map<String, Object> parameters, Date date) {
		RunnableJob rj = runnableJobFactory.runnable(job, parameters);
		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = taskScheduler.schedule(() -> result[0] = rj.run(), date);
		return new JobFuture(jobFuture, () -> result[0] != null ? result[0] : JobResult.unknown(job.getMetadata()));
	}

	@Override
	public ScheduledFuture<?> schedule(Job job, Map<String, Object> parameters, Trigger trigger) {
		RunnableJob rj = runnableJobFactory.runnable(job, parameters);
		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = taskScheduler.schedule(() -> result[0] = rj.run(), trigger);
		return new JobFuture(jobFuture, () -> result[0] != null ? result[0] : JobResult.unknown(job.getMetadata()));
	}
}
