package com.nhl.bootique.job.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.BQModule;
import com.nhl.bootique.env.Environment;
import com.nhl.bootique.factory.FactoryConfigurationService;
import com.nhl.bootique.job.Job;
import com.nhl.bootique.job.command.ExecCommand;
import com.nhl.bootique.job.command.ListCommand;
import com.nhl.bootique.job.command.ScheduleCommand;
import com.nhl.bootique.job.lock.LocalLockHandler;
import com.nhl.bootique.job.lock.LockHandler;
import com.nhl.bootique.job.lock.LockType;
import com.nhl.bootique.job.lock.ZkClusterLockHandler;
import com.nhl.bootique.job.scheduler.Scheduler;
import com.nhl.bootique.job.scheduler.SchedulerFactory;

public class JobBundle {

	private static final String CONFIG_PREFIX = "scheduler";

	private Collection<Class<? extends Job>> jobTypes;
	private boolean useZookeeperLocks;
	private String configPrefix;

	@SafeVarargs
	public static JobBundle create(Class<? extends Job>... jobTypes) {
		return new JobBundle(CONFIG_PREFIX).jobs(jobTypes);
	}

	@SafeVarargs
	public static JobBundle create(String configPrefix, Class<? extends Job>... jobTypes) {
		return new JobBundle(configPrefix).jobs(jobTypes);
	}

	@SafeVarargs
	public static Module module(Class<? extends Job>... jobTypes) {
		return create(jobTypes).module();
	}

	@SafeVarargs
	public static Module module(String configPrefix, Class<? extends Job>... jobTypes) {
		return create(configPrefix, jobTypes).module();
	}

	private JobBundle(String configPrefix) {
		this.jobTypes = new HashSet<>();
		this.configPrefix = configPrefix;
	}

	@SafeVarargs
	public final JobBundle jobs(Class<? extends Job>... jobTypes) {
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

			BQModule.bindCommands(binder, ExecCommand.class, ListCommand.class, ScheduleCommand.class);

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
				FactoryConfigurationService configFactory) {
			return configFactory.factory(SchedulerFactory.class, configPrefix).createScheduler(jobs, environment,
					jobRunners);
		}
	}

}
