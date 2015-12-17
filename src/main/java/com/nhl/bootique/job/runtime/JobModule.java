package com.nhl.bootique.job.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.nhl.bootique.BQBinder;
import com.nhl.bootique.ConfigModule;
import com.nhl.bootique.env.Environment;
import com.nhl.bootique.factory.FactoryConfigurationService;
import com.nhl.bootique.job.Job;
import com.nhl.bootique.job.command.ExecCommand;
import com.nhl.bootique.job.command.ListCommand;
import com.nhl.bootique.job.command.ScheduleCommand;
import com.nhl.bootique.job.lock.LocalLockHandler;
import com.nhl.bootique.job.lock.LockHandler;
import com.nhl.bootique.job.lock.LockType;
import com.nhl.bootique.job.lock.zookeeper.ZkClusterLockHandler;
import com.nhl.bootique.job.scheduler.Scheduler;
import com.nhl.bootique.job.scheduler.SchedulerFactory;

public class JobModule extends ConfigModule {

	private Collection<Class<? extends Job>> jobTypes = new HashSet<>();

	public JobModule() {
	}

	public JobModule(String configPrefix) {
		super(configPrefix);
	}

	@Override
	protected String defaultConfigPrefix() {
		// main config sets up Scheduler , so renaming default config prefix
		return "scheduler";
	}

	@SafeVarargs
	public final JobModule jobs(Class<? extends Job>... jobTypes) {
		Arrays.asList(jobTypes).forEach(jt -> this.jobTypes.add(jt));
		return this;
	}

	@Override
	public void configure(Binder binder) {
		BQBinder.contributeTo(binder).commandTypes(ExecCommand.class, ListCommand.class, ScheduleCommand.class);

		JobBinder.contributeTo(binder).jobTypes(jobTypes);

		MapBinder<LockType, LockHandler> lockHandlers = MapBinder.newMapBinder(binder, LockType.class,
				LockHandler.class);
		lockHandlers.addBinding(LockType.local).to(LocalLockHandler.class);
		lockHandlers.addBinding(LockType.clustered).to(ZkClusterLockHandler.class);
	}

	@Provides
	public Scheduler createScheduler(Set<Job> jobs, Environment environment, Map<LockType, LockHandler> jobRunners,
			FactoryConfigurationService configFactory) {
		return configFactory.factory(SchedulerFactory.class, configPrefix).createScheduler(jobs, environment,
				configFactory, jobRunners);
	}
}
