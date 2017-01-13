package io.bootique.job.scheduler;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJob;
import io.bootique.job.runnable.RunnableJobFactory;
import io.bootique.job.JobRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class DefaultScheduler implements Scheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

	private TaskScheduler taskScheduler;
	private RunnableJobFactory runnableJobFactory;
	private JobRegistry jobRegistry;
	private Collection<TriggerDescriptor> triggers;
	private JobFutures jobFutures;

	public DefaultScheduler(Collection<TriggerDescriptor> triggers,
							TaskScheduler taskScheduler,
							RunnableJobFactory runnableJobFactory,
							JobRegistry jobRegistry) {
		this.triggers = Collections.unmodifiableCollection(triggers);
		this.runnableJobFactory = runnableJobFactory;
		this.taskScheduler = taskScheduler;
		this.jobRegistry = jobRegistry;
		this.jobFutures = new JobFutures();
	}

	@Override
	public int start() {

		if (triggers.isEmpty()) {
			LOGGER.info("No triggers, exiting");
			return 0;
		}

		List<String> badTriggers = triggers.stream().filter(t -> !jobRegistry.getAvailableJobs().contains(t.getJob()))
				.map(t -> t.getJob() + ":" + t.getTrigger()).collect(Collectors.toList());

		if (badTriggers.size() > 0) {
			throw new IllegalStateException("Jobs are not found for the following triggers: " + badTriggers);
		}

		triggers.stream().forEach(tc -> {

			String jobName = tc.getJob();
			LOGGER.info(String.format("Will schedule '%s'.. (%s)", jobName, tc.describeTrigger()));

			Job job = jobRegistry.getJob(tc.getJob());

			schedule(job, Collections.emptyMap(), tc.createTrigger());
		});

		return triggers.size();
	}

	@Override
	public Collection<TriggerDescriptor> getTriggers() {
		return triggers;
	}

	@Override
	public Collection<JobFuture> getSubmittedJobs() {
		return jobFutures.getActiveJobs();
	}

	@Override
	public JobFuture runOnce(String jobName) {
		return runOnce(jobName, Collections.emptyMap());
	}

	@Override
	public JobFuture runOnce(String jobName, Map<String, Object> parameters) {
		Optional<Job> jobOptional = findJobByName(jobName);
		if (jobOptional.isPresent()) {
			Job job = jobOptional.get();
			return runOnce(job, parameters);
		} else {
			return invalidJobNameResult(jobName, parameters);
		}
	}

	private Optional<Job> findJobByName(String jobName) {
		Job job = jobRegistry.getJob(jobName);
		return (job == null) ? Optional.empty() : Optional.of(job);
	}

	private JobFuture invalidJobNameResult(String jobName, Map<String, Object> parameters) {
		return JobFuture.forJob(jobName)
				.future(new ExpiredFuture())
				.runnable(new RunnableJob() {
					@Override
					public JobResult run() {
						return JobResult.unknown(JobMetadata.build(jobName));
					}

					@Override
					public Map<String, Object> getParameters() {
						return parameters;
					}

					@Override
					public boolean isRunning() {
						return false;
					}
				})
				.resultSupplier(() -> JobResult.failure(JobMetadata.build(jobName), "Invalid job name: " + jobName))
				.build();
	}

	@Override
	public JobFuture runOnce(Job job) {
		return runOnce(job, Collections.emptyMap());
	}

	@Override
	public JobFuture runOnce(Job job, Map<String, Object> parameters) {
		return submit(job, parameters,
				(rj, result) -> taskScheduler.schedule(() -> result[0] = rj.run(), new Date()));
	}

	private ScheduledFuture<?> schedule(Job job, Map<String, Object> parameters, Trigger trigger) {
		return submit(job, parameters,
				(rj, result) -> taskScheduler.schedule(() -> result[0] = rj.run(), trigger));
	}

	private JobFuture submit(Job job, Map<String, Object> parameters,
							 BiFunction<RunnableJob, JobResult[], ScheduledFuture<?>> executor) {

		RunnableJob rj = runnableJobFactory.runnable(job, parameters);
		JobResult[] result = new JobResult[1];
		ScheduledFuture<?> jobFuture = executor.apply(rj, result);

		JobFuture future = JobFuture.forJob(job.getMetadata().getName())
				.future(jobFuture)
				.runnable(rj)
				.resultSupplier(() -> result[0] != null ? result[0] : JobResult.unknown(job.getMetadata()))
				.build();

		jobFutures.addFuture(future);
		return future;
	}

	private static class JobFutures {

		private final Queue<JobFuture> submittedJobs;
		private final Collection<JobFuture> runningJobs;
		private final ReentrantLock lock;

		public JobFutures() {
			this.runningJobs = new LinkedList<>();
			this.submittedJobs = new LinkedBlockingQueue<>();
			this.lock = new ReentrantLock();

			// If getActiveJobs() is rarely called, method response time and memory usage will grow over time.
			// To prevent such situation a daemon will perform periodic cleanup of submitted and active jobs.
			Thread cleaner = new Thread(() -> {
				getActiveJobs();
				try {
					Thread.sleep(Duration.ofSeconds(60).toMillis());
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}, "io.bootique.job.Scheduler.JobFutureCleaner");
			cleaner.setDaemon(true);
			cleaner.start();
		}

		public JobFutures addFuture(JobFuture future) {
			submittedJobs.add(future);
			return this;
		}

		public Collection<JobFuture> getActiveJobs() {
			lock.lock();
			try {
				Iterator<JobFuture> iter = runningJobs.iterator();
				while (iter.hasNext()) {
					if (iter.next().isDone()) {
						iter.remove();
					}
				}

				JobFuture submitted;
				while ((submitted = submittedJobs.poll()) != null && !submitted.isDone()) {
					runningJobs.add(submitted);
				}

				// do copying prior to releasing the lock to avoid possible errors
				return new ArrayList<>(runningJobs);
			} finally {
				lock.unlock();
			}
		}
	}
}
