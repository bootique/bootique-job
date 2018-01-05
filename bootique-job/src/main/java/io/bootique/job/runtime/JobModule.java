package io.bootique.job.runtime;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import io.bootique.BQCoreModule;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.job.Job;
import io.bootique.job.JobListener;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class JobModule extends ConfigModule {

    private Collection<Class<? extends Job>> jobTypes = new HashSet<>();

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

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for contributed jobs.
     * @since 0.11
     * @deprecated since 0.14 use {@link #extend(Binder)} to get an extender object, and
     * then call {@link JobModuleExtender#addJob(Job)} or similar.
     */
    @Deprecated
    public static Multibinder<Job> contributeJobs(Binder binder) {
        return Multibinder.newSetBinder(binder, Job.class);
    }

    @Override
    protected String defaultConfigPrefix() {
        // main config sets up Scheduler , so renaming default config prefix
        return "scheduler";
    }

    /**
     * @param jobTypes an array of job classes to register in runtime.
     * @return this module instance.
     * @deprecated since 0.24 use JobModule.extend(..) API to register jobs. Modules in Bootique are normally immutable
     * and extensions are loaded via "extenders".
     */
    @Deprecated
    @SafeVarargs
    public final JobModule jobs(Class<? extends Job>... jobTypes) {
        Arrays.asList(jobTypes).forEach(jt -> this.jobTypes.add(jt));
        return this;
    }

    @Override
    public void configure(Binder binder) {

        BQCoreModule.extend(binder).addCommand(ExecCommand.class)
                .addCommand(ListCommand.class)
                .addCommand(ScheduleCommand.class);

        // trigger extension points creation and provide default contributions
        JobModuleExtender extender = JobModule.extend(binder).initAllExtensions();
        jobTypes.forEach(extender::addJob);

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
            Scheduler scheduler,
            ConfigurationFactory configFactory) {

        TypeRef<Map<String, JobDefinition>> ref = new TypeRef<Map<String, JobDefinition>>() {
        };
        Map<String, JobDefinition> configuredDefinitions = configFactory.config(ref, "jobs");

        return new DefaultJobRegistry(jobs, configuredDefinitions, scheduler, allListeners(jobListeners, mappedJobListeners));
    }

    private Set<MappedJobListener> allListeners(Set<JobListener> jobListeners,
                                                Set<MappedJobListener> mappedJobListeners) {
        if (jobListeners.isEmpty()) {
            return mappedJobListeners;
        }

        HashSet<MappedJobListener> mappedListenersClone = new HashSet<>(mappedJobListeners);

        //  Integer.MAX_VALUE means placing bare unordered listeners after (== inside) mapped listeners
        jobListeners.forEach(
                listener -> {
                    mappedListenersClone.add(new MappedJobListener<>(listener, Integer.MAX_VALUE));
                }
        );

        return sortedListeners(mappedListenersClone);
    }

    private Set<MappedJobListener> sortedListeners(Set<MappedJobListener> unsorted) {
        Set<MappedJobListener> sorted = new TreeSet<>(Comparator.comparing(MappedJobListener::getOrder));

        sorted.addAll(unsorted);
        return sorted;
    }
}
