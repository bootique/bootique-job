package com.nhl.bootique.job.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.BQModule;
import com.nhl.bootique.FactoryModule;
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

public class JobModule extends FactoryModule<SchedulerFactory> {

	@SafeVarargs
	public static void bindJobTypes(Binder binder, Class<? extends Job>... jobTypes) {
		Preconditions.checkNotNull(jobTypes);
		bindJobTypes(binder, Arrays.asList(jobTypes));
	}

	public static void bindJobTypes(Binder binder, Collection<Class<? extends Job>> jobTypes) {
		Multibinder<Job> jobBinder = Multibinder.newSetBinder(binder, Job.class);
		jobTypes.forEach(jt -> jobBinder.addBinding().to(jt).in(Singleton.class));
	}

	private Collection<Class<? extends Job>> jobTypes = new HashSet<>();

	public JobModule() {
		super(SchedulerFactory.class);
	}

	public JobModule(String configPrefix) {
		super(SchedulerFactory.class, configPrefix);
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
		BQModule.bindCommandTypes(binder, ExecCommand.class, ListCommand.class, ScheduleCommand.class);
		JobModule.bindJobTypes(binder, jobTypes);
		MapBinder<LockType, LockHandler> lockHandlers = MapBinder.newMapBinder(binder, LockType.class,
				LockHandler.class);
		lockHandlers.addBinding(LockType.local).to(LocalLockHandler.class);
		lockHandlers.addBinding(LockType.clustered).to(ZkClusterLockHandler.class);
	}

	@Provides
	public Scheduler createScheduler(Set<Job> jobs, Environment environment, Map<LockType, LockHandler> jobRunners,
			FactoryConfigurationService configFactory) {
		return createFactory(configFactory).createScheduler(jobs, environment, jobRunners);
	}
}
