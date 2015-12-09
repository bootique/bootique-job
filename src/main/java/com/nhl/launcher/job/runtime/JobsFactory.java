package com.nhl.launcher.job.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.nhl.launcher.env.Environment;
import com.nhl.launcher.job.Job;
import com.nhl.launcher.job.lock.LockHandler;
import com.nhl.launcher.job.lock.LockType;
import com.nhl.launcher.job.runnable.ErrorHandlingRunnableJobFactory;
import com.nhl.launcher.job.runnable.LockAwareRunnableJobFactory;
import com.nhl.launcher.job.runnable.RunnableJobFactory;
import com.nhl.launcher.job.runnable.SimpleRunnableJobFactory;
import com.nhl.launcher.job.scheduler.DefaultScheduler;
import com.nhl.launcher.job.scheduler.Scheduler;
import com.nhl.launcher.job.scheduler.TriggerDescriptor;

/**
 * A configuration object that is used to setup jobs runtime.
 */
public class JobsFactory {

	private String jobPropertiesPrefix;
	private Collection<TriggerDescriptor> triggers;
	private int threadPoolSize;
	private boolean clusteredLocks;

	public JobsFactory() {
		this.triggers = new ArrayList<>();
		this.threadPoolSize = 3;
	}

	public Scheduler createScheduler(Set<Job> jobs, Environment environment, Map<LockType, LockHandler> jobRunners) {

		TaskScheduler taskScheduler = createTaskScheduler();

		LockType lockType = clusteredLocks ? LockType.clustered : LockType.local;
		LockHandler lockHandler = jobRunners.get(lockType);

		if (lockHandler == null) {
			throw new IllegalStateException("No SerialJobRunner for lock type: " + lockType);
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
