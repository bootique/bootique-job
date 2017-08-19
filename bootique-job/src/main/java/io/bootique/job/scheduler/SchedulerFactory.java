package io.bootique.job.scheduler;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.lock.LockType;
import io.bootique.job.runnable.ErrorHandlingRunnableJobFactory;
import io.bootique.job.runnable.LockAwareRunnableJobFactory;
import io.bootique.job.runnable.RunnableJobFactory;
import io.bootique.job.runnable.SimpleRunnableJobFactory;
import io.bootique.job.JobRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * A configuration object that is used to setup jobs runtime.
 */
@BQConfig("Job scheduler/executor.")
public class SchedulerFactory {

	private Collection<TriggerDescriptor> triggers;
	private int threadPoolSize;
	private boolean clusteredLocks;

	public SchedulerFactory() {
		this.triggers = new ArrayList<>();
		this.threadPoolSize = 4;
	}

	public Scheduler createScheduler(Map<LockType, LockHandler> lockHandlers,
									 JobRegistry jobRegistry) {

		TaskScheduler taskScheduler = createTaskScheduler();

		LockType lockType = clusteredLocks ? LockType.clustered : LockType.local;
		LockHandler lockHandler = lockHandlers.get(lockType);

		if (lockHandler == null) {
			throw new IllegalStateException("No LockHandler for lock type: " + lockType);
		}

		RunnableJobFactory rf1 = new SimpleRunnableJobFactory();
		RunnableJobFactory rf2 = new LockAwareRunnableJobFactory(rf1, lockHandler, jobRegistry);
		RunnableJobFactory rf3 = new ErrorHandlingRunnableJobFactory(rf2);

		// TODO: write a builder instead of this insane constructor
		return new DefaultScheduler(triggers, taskScheduler, rf3, jobRegistry);
	}

	protected TaskScheduler createTaskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(threadPoolSize);
		taskScheduler.initialize();
		return taskScheduler;
	}

	@BQConfigProperty("Collection of job triggers.")
	public void setTriggers(Collection<TriggerDescriptor> triggers) {
		this.triggers = triggers;
	}

	@BQConfigProperty("Minimum number of workers to keep alive (and not allow to time out etc)." +
			" Should be 1 or higher. Default value is 1.")
	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	@BQConfigProperty("Determines whether the lock handlers will be aware of the Zookeeper cluster.")
	public void setClusteredLocks(boolean clusteredLocks) {
		this.clusteredLocks = clusteredLocks;
	}
}
