package com.nhl.bootique.job.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.nhl.bootique.env.Environment;
import com.nhl.bootique.job.Job;
import com.nhl.bootique.job.lock.LockHandler;
import com.nhl.bootique.job.lock.LockType;
import com.nhl.bootique.job.runnable.ErrorHandlingRunnableJobFactory;
import com.nhl.bootique.job.runnable.LockAwareRunnableJobFactory;
import com.nhl.bootique.job.runnable.RunnableJobFactory;
import com.nhl.bootique.job.runnable.SimpleRunnableJobFactory;

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
		this.threadPoolSize = 3;
	}

	public Scheduler createScheduler(Set<Job> jobs, Environment environment, Map<LockType, LockHandler> lockHandlers) {

		TaskScheduler taskScheduler = createTaskScheduler();

		LockType lockType = clusteredLocks ? LockType.clustered : LockType.local;
		LockHandler lockHandler = lockHandlers.get(lockType);

		if (lockHandler == null) {
			throw new IllegalStateException("No LockHandler for lock type: " + lockType);
		}

		RunnableJobFactory rf1 = new SimpleRunnableJobFactory();
		RunnableJobFactory rf2 = new LockAwareRunnableJobFactory(rf1, lockHandler);
		RunnableJobFactory rf3 = new ErrorHandlingRunnableJobFactory(rf2);

		// TODO: write a builder instead of this insane constructor
		return new DefaultScheduler(jobs, triggers, taskScheduler, rf3, environment, jobPropertiesPrefix);
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
