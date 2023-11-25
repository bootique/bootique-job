/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.job;

import io.bootique.BQCoreModule;
import io.bootique.BQModuleProvider;
import io.bootique.bootstrap.BuiltModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Injector;
import io.bootique.di.Provides;
import io.bootique.help.ValueObjectDescriptor;
import io.bootique.jackson.JacksonService;
import io.bootique.job.command.ExecCommand;
import io.bootique.job.command.ListCommand;
import io.bootique.job.command.ScheduleCommand;
import io.bootique.job.graph.JobGraphNodeFactory;
import io.bootique.job.lock.LocalLockHandler;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.runtime.*;
import io.bootique.job.scheduler.SchedulerFactory;
import io.bootique.job.trigger.JobExecParser;
import io.bootique.job.value.Cron;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.shutdown.ShutdownManager;
import io.bootique.type.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;

public class JobModule implements BQModule, BQModuleProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobModule.class);

    static final String JOBS_CONFIG_PREFIX = "jobs";
    static final String SCHEDULER_CONFIG_PREFIX = "scheduler";

    public static final String JOB_OPTION = "job";

    /**
     * Returns an instance of {@link JobModuleExtender} used by downstream modules to load custom extensions to the
     * JobModule. Should be invoked from a downstream Module's "configure" method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link JobModuleExtender} that can be used to load custom extensions to the JobModule.
     */
    public static JobModuleExtender extend(Binder binder) {
        return new JobModuleExtender(binder);
    }

    @Override
    public BuiltModule buildModule() {
        TypeRef<Map<String, JobGraphNodeFactory>> jobs = new TypeRef<>() {};

        return BuiltModule.of(this)
                .description("Provides Bootique's own job execution engine")
                .config("scheduler", SchedulerFactory.class)
                .config("jobs", jobs.getType())
                .build();
    }

    @Override
    public void configure(Binder binder) {

        JobModule.extend(binder).initAllExtensions();

        // binding via provider, to simplify overriding in the "bootique-job-instrumented" module
        binder.bind(JobRegistry.class).toProvider(JobRegistryProvider.class).inSingletonScope();

        BQCoreModule.extend(binder).addCommand(ExecCommand.class)
                .addOption(OptionMetadata.builder(JOB_OPTION).description("Specifies the name of a job to execute or schedule. "
                                + "Job name may be optionally followed by a JSON map containing job parameters (e.g. 'myjob{\"p\":1}') "
                                + "Used in conjunction with '--execute' or '--schedule' commands. "
                                + "Available job names can be viewed using '--list' command.")
                        .valueRequired("job_name")
                        .build())
                .addCommand(ListCommand.class)
                .addCommand(ScheduleCommand.class)
                .addValueObjectDescriptor(Cron.class, new ValueObjectDescriptor("6-part cron expression, e.g. '0 0 * * * *'"));
    }

    @Provides
    @Singleton
    JobExecParser createExecParser(JobRegistry registry, JacksonService jackson) {
        return new JobExecParser(registry, jackson.newObjectMapper());
    }

    @Provides
    @Singleton
    Scheduler createScheduler(
            JobRegistry jobRegistry,
            JobDecorators decorators,
            ConfigurationFactory configFactory,
            ShutdownManager shutdownManager) {

        return configFactory.config(SchedulerFactory.class, SCHEDULER_CONFIG_PREFIX).createScheduler(jobRegistry, decorators, shutdownManager);
    }

    // this is a secondary thread pool used for graph execution
    @Provides
    @Singleton
    GraphExecutor createGraphExecutor(
            ConfigurationFactory configFactory,
            Injector injector,
            ShutdownManager shutdownManager) {
        return configFactory.config(SchedulerFactory.class, SCHEDULER_CONFIG_PREFIX).createGraphExecutor(injector, shutdownManager);
    }

    @Provides
    @Singleton
    JobDecorators provideDecorators(
            LockHandler lockHandler,
            JobLogger jobLogger,
            JobListenersDispatcherDecorator listenerDispatcher,
            Set<JobDecorator> decorators,
            Set<MappedJobDecorator<?>> mappedDecorators) {

        return JobDecorators.builder()
                .add(decorators)
                .addMapped(mappedDecorators)
                .exceptionHandler(new ExceptionsHandlerDecorator())
                .logger(jobLogger)
                .lockHandler(lockHandler)
                .listenerDispatcher(listenerDispatcher)
                .renamer(new JobNameDecorator())
                .paramsBinder(new JobParamsBinderDecorator())
                .create();
    }

    @Provides
    @Singleton
    LockHandler provideLockHandler(Set<LockHandler> lockHandlers) {
        switch (lockHandlers.size()) {
            case 0:
                // only use the default lock handler if none is provided by other modules
                return new LocalLockHandler();
            case 1:
                LOGGER.info("Using '{}' lock handler", lockHandlers.iterator().next());
                return lockHandlers.iterator().next();
            default:
                throw new RuntimeException("There's more than one LockHandler defined. Can't determine the default: " + lockHandlers);
        }
    }

    @Provides
    @Singleton
    JobLogger provideJobLogger() {
        return new JobLogger();
    }

    @Provides
    @Singleton
    JobListenersDispatcherDecorator provideListenerDecorator(Set<JobListener> listeners, Set<MappedJobListener> mappedListeners) {
        // not checking for dupes between MappedJobListener and JobListener collections. Is that a problem?
        List<MappedJobListener> localListeners = new ArrayList<>(mappedListeners.size() + listeners.size());
        localListeners.addAll(mappedListeners);

        //  Integer.MAX_VALUE means placing bare unordered listeners after (== inside) mapped listeners
        listeners.forEach(listener -> localListeners.add(new MappedJobListener<>(listener, Integer.MAX_VALUE)));
        localListeners.sort(Comparator.comparing(MappedJobListener::getOrder));

        return new JobListenersDispatcherDecorator(localListeners);
    }
}
