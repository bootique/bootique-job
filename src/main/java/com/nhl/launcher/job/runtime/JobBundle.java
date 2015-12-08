package com.nhl.launcher.job.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.nhl.launcher.command.Command;
import com.nhl.launcher.config.ConfigurationFactory;
import com.nhl.launcher.env.Environment;
import com.nhl.launcher.job.Job;
import com.nhl.launcher.job.command.ExecCommand;
import com.nhl.launcher.job.command.ListCommand;
import com.nhl.launcher.job.lock.LocalLockHandler;
import com.nhl.launcher.job.lock.LockHandler;
import com.nhl.launcher.job.lock.LockType;
import com.nhl.launcher.job.lock.ZkClusterLockHandler;
import com.nhl.launcher.job.runnable.ErrorHandlingRunnableJobFactory;
import com.nhl.launcher.job.runnable.RunnableJobFactory;
import com.nhl.launcher.job.runnable.LockAwareRunnableJobFactory;
import com.nhl.launcher.job.runnable.SimpleRunnableJobFactory;
import com.nhl.launcher.job.scheduler.DefaultScheduler;
import com.nhl.launcher.job.scheduler.Scheduler;

public class JobBundle {

	private static final String CONFIG_PREFIX = "scheduler";

	private Collection<Class<? extends Job>> jobTypes;
	private boolean useZookeeperLocks;
	private String configPrefix;

	@SafeVarargs
	public static JobBundle jobs(Class<? extends Job>... jobTypes) {
		return new JobBundle(CONFIG_PREFIX).addJobs(jobTypes);
	}

	@SafeVarargs
	public static JobBundle jobs(String configPrefix, Class<? extends Job>... jobTypes) {
		return new JobBundle(configPrefix).addJobs(jobTypes);
	}

	private JobBundle(String configPrefix) {
		this.jobTypes = new HashSet<>();
		this.configPrefix = configPrefix;
	}

	@SafeVarargs
	public final JobBundle addJobs(Class<? extends Job>... jobTypes) {
		Arrays.asList(jobTypes).forEach(jt -> this.jobTypes.add(jt));
		return this;
	}

	public JobBundle useZookeeperLocks() {
		this.useZookeeperLocks = true;
		return this;
	}

	public Module module() {
		return new JobModule();
	}

	class JobModule implements Module {

		@Override
		public void configure(Binder binder) {

			Multibinder.newSetBinder(binder, Command.class).addBinding().to(ExecCommand.class);
			Multibinder.newSetBinder(binder, Command.class).addBinding().to(ListCommand.class);

			jobTypes.forEach(jt -> Multibinder.newSetBinder(binder, Job.class).addBinding().to(jt).in(Singleton.class));

			MapBinder<LockType, LockHandler> serialJobRunners = MapBinder.newMapBinder(binder, LockType.class,
					LockHandler.class);
			serialJobRunners.addBinding(LockType.local).to(LocalLockHandler.class);

			if (useZookeeperLocks) {
				serialJobRunners.addBinding(LockType.clustered).to(ZkClusterLockHandler.class);
			}
		}

		@Provides
		public Scheduler createScheduler(Set<Job> jobs, Environment environment, Map<LockType, LockHandler> jobRunners,
				ConfigurationFactory configFactory) {

			JobsConfig config = configFactory.subconfig(configPrefix, JobsConfig.class);
			TaskScheduler taskScheduler = createTaskScheduler(config);

			LockType lockType = config.isClusteredLocks() ? LockType.clustered : LockType.local;
			LockHandler lockHandler = jobRunners.get(lockType);

			if (lockHandler == null) {
				throw new IllegalStateException("No SerialJobRunner for lock type: " + lockType);
			}

			RunnableJobFactory rf1 = new SimpleRunnableJobFactory();
			RunnableJobFactory rf2 = new LockAwareRunnableJobFactory(rf1, lockHandler);
			RunnableJobFactory rf3 = new ErrorHandlingRunnableJobFactory(rf2);

			// TODO: write a builder instead of this insane constructor
			return new DefaultScheduler(jobs, config.getTriggers(), taskScheduler, rf3, environment,
					config.getJobPropertiesPrefix());
		}

		private TaskScheduler createTaskScheduler(JobsConfig config) {
			ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
			taskScheduler.setPoolSize(config.getThreadPoolSize());
			taskScheduler.initialize();
			return taskScheduler;
		}
	}

}
