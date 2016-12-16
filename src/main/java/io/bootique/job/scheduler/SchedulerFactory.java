package io.bootique.job.scheduler;

import io.bootique.config.ConfigurationFactory;
import io.bootique.env.Environment;
import io.bootique.job.Job;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.SingleJob;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.lock.LockType;
import io.bootique.job.runnable.ErrorHandlingRunnableJobFactory;
import io.bootique.job.runnable.LockAwareRunnableJobFactory;
import io.bootique.job.runnable.RunnableJobFactory;
import io.bootique.job.runnable.SimpleRunnableJobFactory;
import io.bootique.type.TypeRef;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A configuration object that is used to setup jobs runtime.
 */
public class SchedulerFactory {

	private String jobPropertiesPrefix;
	private Collection<TriggerDescriptor> triggers;
	private int threadPoolSize;
	private boolean clusteredLocks;

	public SchedulerFactory() {
		this.triggers = new ArrayList<>();
		this.threadPoolSize = 4;
		this.jobPropertiesPrefix = "jobs";
	}

	public Scheduler createScheduler(Set<Job> jobs,
									 Environment environment,
									 ConfigurationFactory configFactory,
									 Map<LockType, LockHandler> lockHandlers) {

		TaskScheduler taskScheduler = createTaskScheduler();

		LockType lockType = clusteredLocks ? LockType.clustered : LockType.local;
		LockHandler lockHandler = lockHandlers.get(lockType);

		if (lockHandler == null) {
			throw new IllegalStateException("No LockHandler for lock type: " + lockType);
		}

		RunnableJobFactory rf1 = new SimpleRunnableJobFactory();
		RunnableJobFactory rf2 = new LockAwareRunnableJobFactory(rf1, lockHandler);
		RunnableJobFactory rf3 = new ErrorHandlingRunnableJobFactory(rf2);

		Map<String, JobDefinition> jobDefinitions = collectJobDefinitions(jobs, configFactory);

		// TODO: write a builder instead of this insane constructor
		return new DefaultScheduler(jobs, triggers, taskScheduler, rf3, jobDefinitions);
	}

	private Map<String, JobDefinition> collectJobDefinitions(Set<Job> jobs, ConfigurationFactory configFactory) {
		Map<String, JobDefinition> jobDefinitions = createJobProperties(configFactory);
		// create definition for each job, that is not present in config
		jobs.stream().filter(job -> !jobDefinitions.containsKey(job.getMetadata().getName())).forEach(job -> {
			jobDefinitions.put(job.getMetadata().getName(), new SingleJob());
		});
		return jobDefinitions;
	}

	protected Map<String, JobDefinition> createJobProperties(ConfigurationFactory configFactory) {
		return configFactory.config(new TypeRef<Map<String,JobDefinition>>() {
		}, jobPropertiesPrefix);
	}

	protected TaskScheduler createTaskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(threadPoolSize);
		taskScheduler.initialize();
		return taskScheduler;
	}

	public void setTriggers(Collection<TriggerDescriptor> triggers) {
		this.triggers = triggers;
	}

	public void setJobPropertiesPrefix(String propertiesNamespace) {
		this.jobPropertiesPrefix = propertiesNamespace;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public void setClusteredLocks(boolean clusteredLocks) {
		this.clusteredLocks = clusteredLocks;
	}
}
