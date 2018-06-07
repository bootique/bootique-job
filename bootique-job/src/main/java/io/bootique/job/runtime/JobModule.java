package io.bootique.job.runtime;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import io.bootique.BQCoreModule;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.job.Job;
import io.bootique.job.JobListener;
import io.bootique.job.JobLogListener;
import io.bootique.job.JobRegistry;
import io.bootique.job.MappedJobListener;
import io.bootique.job.command.ExecCommand;
import io.bootique.job.command.ListCommand;
import io.bootique.job.command.ScheduleCommand;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.lock.LocalLockHandler;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.lock.LockType;
import io.bootique.job.lock.zookeeper.ZkClusterLockHandler;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.scheduler.SchedulerFactory;
import io.bootique.job.scheduler.execution.DefaultJobRegistry;
import io.bootique.shutdown.ShutdownManager;
import io.bootique.type.TypeRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JobModule extends ConfigModule {

    // TX ID listener is usually the outermost listener in any app. It is a good idea to order your other listeners
    // relative to this one , using higher ordering values.
    public static final int BUSINESS_TX_LISTENER_ORDER = Integer.MIN_VALUE + 800;

    // goes inside BUSINESS_TX_LISTENER
    public static final int LOG_LISTENER_ORDER = BUSINESS_TX_LISTENER_ORDER + 200;

    public JobModule() {
    }

    public JobModule(String configPrefix) {
        super(configPrefix);
    }

    /**
     * Returns an instance of {@link JobModuleExtender} used by downstream modules to load custom extensions to the
     * JobModule. Should be invoked from a downstream Module's "configure" method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link JobModuleExtender} that can be used to load custom extensions to the JobModule.
     * @since 0.14
     */
    public static JobModuleExtender extend(Binder binder) {
        return new JobModuleExtender(binder);
    }

    @Override
    protected String defaultConfigPrefix() {
        // main config sets up Scheduler , so renaming default config prefix
        return "scheduler";
    }

    @Override
    public void configure(Binder binder) {

        BQCoreModule.extend(binder).addCommand(ExecCommand.class)
                .addCommand(ListCommand.class)
                .addCommand(ScheduleCommand.class);

        JobModuleExtender extender = JobModule
                .extend(binder)
                .initAllExtensions()
                .addMappedListener(new TypeLiteral<MappedJobListener<JobLogListener>>() {});

        // TODO: move this to extender API
        MapBinder<LockType, LockHandler> lockHandlers = MapBinder.newMapBinder(binder, LockType.class,
                LockHandler.class);
        lockHandlers.addBinding(LockType.local).to(LocalLockHandler.class);
        lockHandlers.addBinding(LockType.clustered).to(ZkClusterLockHandler.class);
    }

    @Provides
    @Singleton
    Scheduler createScheduler(
            Map<LockType, LockHandler> jobRunners,
            JobRegistry jobRegistry,
            ConfigurationFactory configFactory,
            ShutdownManager shutdownManager) {

        return configFactory.config(SchedulerFactory.class, configPrefix)
                .createScheduler(jobRunners, jobRegistry, shutdownManager);
    }

    @Provides
    @Singleton
    JobRegistry createJobRegistry(
            Set<Job> jobs,
            Set<JobListener> jobListeners,
            Set<MappedJobListener> mappedJobListeners,
            Provider<Scheduler> schedulerProvider,
            ConfigurationFactory configFactory) {

        TypeRef<Map<String, JobDefinition>> ref = new TypeRef<Map<String, JobDefinition>>() {
        };
        Map<String, JobDefinition> configuredDefinitions = configFactory.config(ref, "jobs");

        List<MappedJobListener> allListeners = allListeners(jobListeners, mappedJobListeners);
        return new DefaultJobRegistry(jobs, configuredDefinitions, schedulerProvider, allListeners);
    }

    @Singleton
    @Provides
    public MappedJobListener<JobLogListener> provideJobLogListener() {
        return new MappedJobListener<>(new JobLogListener(), LOG_LISTENER_ORDER);
    }

    private List<MappedJobListener> allListeners(
            Set<JobListener> jobListeners,
            Set<MappedJobListener> mappedJobListeners) {

        // not checking for dupes between MappedJobListener and JobListener collections. Is that a problem?
        List<MappedJobListener> localListeners = new ArrayList<>(mappedJobListeners.size() + jobListeners.size());

        localListeners.addAll(mappedJobListeners);

        //  Integer.MAX_VALUE means placing bare unordered listeners after (== inside) mapped listeners
        jobListeners.forEach(
                listener -> localListeners.add(new MappedJobListener<>(listener, Integer.MAX_VALUE))
        );

        Collections.sort(localListeners, Comparator.comparing(MappedJobListener::getOrder));

        return localListeners;
    }
}
