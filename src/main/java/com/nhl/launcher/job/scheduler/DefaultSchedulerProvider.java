package com.nhl.launcher.job.scheduler;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.nhl.launcher.config.ConfigurationFactory;
import com.nhl.launcher.env.Environment;
import com.nhl.launcher.job.Job;
import com.nhl.launcher.job.locking.LockType;
import com.nhl.launcher.job.locking.SerialJobRunner;
import com.nhl.launcher.job.runnable.ErrorHandlingRunnableJobFactory;
import com.nhl.launcher.job.runnable.RunnableJobFactory;
import com.nhl.launcher.job.runnable.RunnableSerialJobFactory;
import com.nhl.launcher.job.runnable.SimpleRunnableJobFactory;

public class DefaultSchedulerProvider implements Provider<Scheduler> {

	private static final String CONFIG_PREFIX = "scheduler";

	private Collection<Job> jobs;
	private Map<LockType, SerialJobRunner> jobRunners;
	private Environment environment;
	private ConfigurationFactory configFactory;

	@Inject
	public DefaultSchedulerProvider(Set<Job> jobs, Environment environment, Map<LockType, SerialJobRunner> jobRunners,
			ConfigurationFactory configFactory) {

		this.jobs = jobs;
		this.jobRunners = jobRunners;
		this.environment = environment;
		this.configFactory = configFactory;
	}

	@Override
	public Scheduler get() {

		SchedulerConfig config = configFactory.subconfig(CONFIG_PREFIX, SchedulerConfig.class);
		TaskScheduler taskScheduler = createTaskScheduler(config);

		LockType lockType = config.isClusteredLocks() ? LockType.clustered : LockType.local;
		SerialJobRunner serialRunner = jobRunners.get(lockType);

		if (serialRunner == null) {
			throw new IllegalStateException("No SerialJobRunner for lock type: " + lockType);
		}

		RunnableJobFactory rf1 = new SimpleRunnableJobFactory();
		RunnableJobFactory rf2 = new RunnableSerialJobFactory(rf1, serialRunner);
		RunnableJobFactory rf3 = new ErrorHandlingRunnableJobFactory(rf2);

		// TODO: write a builder instead of this insane constructor
		return new DefaultScheduler(jobs, config.getTriggers(), taskScheduler, rf3, environment,
				config.getJobPropertiesPrefix());
	}

	protected TaskScheduler createTaskScheduler(SchedulerConfig config) {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(config.getThreadPoolSize());
		taskScheduler.initialize();
		return taskScheduler;
	}
}
