package com.nhl.launcher.job.scheduler;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import com.nhl.launcher.job.Job;
import com.nhl.launcher.job.JobMetadata;
import com.nhl.launcher.job.JobMetadataBuilder;
import com.nhl.launcher.job.JobParameterMetadata;
import com.nhl.launcher.job.runnable.JobFuture;
import com.nhl.launcher.job.runnable.JobResult;
import com.nhl.launcher.job.runnable.RunnableJob;
import com.nhl.launcher.job.runnable.RunnableJobFactory;

public class DefaultScheduler implements Scheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

	private TaskScheduler scheduler;
	private SchedulerConfig config;
	private RunnableJobFactory runnableJobFactory;
	private Collection<Job> jobs;

	public DefaultScheduler(SchedulerConfig config, TaskScheduler scheduler, Collection<Job> jobs,
			RunnableJobFactory runnableJobFactory) {

		this.config = config;
		this.scheduler = scheduler;
		this.jobs = jobs;
		this.runnableJobFactory = runnableJobFactory;
	}

	private Map<String, Job> mapJobs() {
		return jobs.stream().collect(toMap(j -> j.getMetadata().getName(), j -> j));
	}

	@Override
	public JobFuture runOnce(String jobName) {
		Map<String, Job> jobs = mapJobs();

		Job job = jobs.get(jobName);
		if (job == null) {
			return new JobFuture(new ExpiredFuture(), () -> JobResult.failure(JobMetadataBuilder.build(jobName)));
		}

		Map<String, Object> parameters = jobParams(job);
		RunnableJob rj = runnableJobFactory.runnable(job, parameters);

		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = scheduler.schedule(() -> result[0] = rj.run(), new Date());
		return new JobFuture(jobFuture, () -> result[0] != null ? result[0] : JobResult.unknown(job.getMetadata()));
	}

	@Override
	public int start() {

		if (config.getTriggers().isEmpty()) {
			LOGGER.info("No triggers, exiting");
			return 0;
		}

		Map<String, Job> jobs = mapJobs();

		List<String> badTriggers = config.getTriggers().stream().filter(t -> !jobs.containsKey(t.getJob()))
				.map(t -> t.getJob() + ":" + t.getTrigger()).collect(Collectors.toList());

		if (badTriggers.size() > 0) {
			throw new IllegalStateException("Jobs are not found for the following triggers: " + badTriggers);
		}

		config.getTriggers().stream().forEach(tc -> {

			Job job = jobs.get(tc.getJob());
			Map<String, Object> parameters = jobParams(job);

			LOGGER.info(String.format("Will schedule '%s'.. (%s)", job.getMetadata().getName(), tc.describeTrigger()));

			RunnableJob rj = runnableJobFactory.runnable(job, parameters);
			scheduler.schedule(() -> rj.run(), tc.createTrigger());
		});

		return config.getTriggers().size();
	}

	Map<String, Object> jobParams(Job job) {

		// Taking parameters from Environment (i.e. System, etc.). Meaning
		// parameters can be passed as -Dp1=v1 -Dp2=v2 .. also can be bound in
		// YAML.

		// Property name is built using the following namespace convention:
		// <prefix>.jobname.paraname ("prefix" is usually "jobs").

		Map<String, Object> params = new HashMap<>();
		for (JobParameterMetadata<?> param : job.getMetadata().getParameters()) {

			String propertyName = toPropertyName(job.getMetadata(), param);
			String valueString = environment.getProperty(propertyName);
			Object value = param.fromString(valueString);
			params.put(param.getName(), value);
		}

		return params;
	}

	private String toPropertyName(JobMetadata jobMD, JobParameterMetadata<?> parameterMD) {
		String prefix = config.getJobPropertiesPrefix() == null ? "" : config.getJobPropertiesPrefix() + ".";
		return prefix + jobMD.getName() + "." + parameterMD.getName();
	}

	private final class ExpiredFuture implements ScheduledFuture<Object> {

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public Object get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed o) {
			return -(int) o.getDelay(TimeUnit.SECONDS);
		}
	}
}
