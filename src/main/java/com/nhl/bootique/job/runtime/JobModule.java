package com.nhl.bootique.job.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.BQCoreModule;
import com.nhl.bootique.ConfigModule;
import com.nhl.bootique.command.Command;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.env.Environment;
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

	/**
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.11
	 * @return returns a {@link Multibinder} for contributed jobs.
	 */
	public static Multibinder<Job> contributeJobs(Binder binder) {
		return Multibinder.newSetBinder(binder, Job.class);
	}

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

		Multibinder<Command> commandBinder = BQCoreModule.contributeCommands(binder);
		commandBinder.addBinding().to(ExecCommand.class).in(Singleton.class);
		commandBinder.addBinding().to(ListCommand.class).in(Singleton.class);
		commandBinder.addBinding().to(ScheduleCommand.class).in(Singleton.class);

		// trigger extension points creation and provide default contributions

		Multibinder<Job> jobBinder = JobModule.contributeJobs(binder);
		jobTypes.forEach(jt -> jobBinder.addBinding().to(jt).in(Singleton.class));

		MapBinder<LockType, LockHandler> lockHandlers = MapBinder.newMapBinder(binder, LockType.class,
				LockHandler.class);
		lockHandlers.addBinding(LockType.local).to(LocalLockHandler.class);
		lockHandlers.addBinding(LockType.clustered).to(ZkClusterLockHandler.class);
	}

	@Provides
	protected Scheduler createScheduler(Set<Job> jobs, Environment environment, Map<LockType, LockHandler> jobRunners,
			ConfigurationFactory configFactory) {
		return configFactory.config(SchedulerFactory.class, configPrefix).createScheduler(jobs, environment,
				configFactory, jobRunners);
	}
}
