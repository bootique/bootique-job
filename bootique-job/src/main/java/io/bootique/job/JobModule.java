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
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.di.TypeLiteral;
import io.bootique.help.ValueObjectDescriptor;
import io.bootique.job.command.ExecCommand;
import io.bootique.job.command.ListCommand;
import io.bootique.job.command.ScheduleCommand;
import io.bootique.job.lock.LocalLockHandler;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.runtime.*;
import io.bootique.job.scheduler.SchedulerFactory;
import io.bootique.job.value.Cron;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.shutdown.ShutdownManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JobModule extends ConfigModule {

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
    protected String defaultConfigPrefix() {
        // main config sets up Scheduler, so renaming default config prefix
        return SCHEDULER_CONFIG_PREFIX;
    }

    @Override
    public void configure(Binder binder) {

        JobModule.extend(binder).initAllExtensions()
                .addMappedDecorator(new TypeLiteral<MappedJobDecorator<JobLogger>>() {
                })
                .addDecorator(new ExceptionsHandlerDecorator(), JobDecorators.JOB_EXCEPTIONS_HANDLER_DECORATOR_ORDER)
                .addMappedDecorator(new TypeLiteral<MappedJobDecorator<LockHandler>>() {
                })
                .addDecorator(new JobParamDefaultsDecorator(), JobDecorators.PARAM_DEFAULTS_DECORATOR_ORDER)
                // TODO: Listeners should be changed to Decorators (can be done in backwards-compatible manner)
                .addMappedDecorator(new TypeLiteral<MappedJobDecorator<JobListenerDecorator>>() {
                })
                .addDecorator(new JobNameDecorator(), JobDecorators.JOB_NAME_DECORATOR_ORDER)
        ;

        // binding via provider, to simplify overriding in the "bootique-job-instrumented" module
        binder.bind(JobRegistry.class).toProvider(JobRegistryProvider.class).inSingletonScope();

        BQCoreModule.extend(binder).addCommand(ExecCommand.class)
                .addOption(OptionMetadata.builder(JOB_OPTION).description("Specifies the name of the job to execute or schedule. "
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
    Scheduler createScheduler(
            JobRegistry jobRegistry,
            JobDecorators decorators,
            ConfigurationFactory configFactory,
            ShutdownManager shutdownManager) {

        return config(SchedulerFactory.class, configFactory).createScheduler(jobRegistry, decorators, shutdownManager);
    }

    @Provides
    @Singleton
    JobDecorators provideDecorators(Set<JobDecorator> decorators, Set<MappedJobDecorator> mappedDecorators) {

        List<MappedJobDecorator> localDecorators = new ArrayList<>(mappedDecorators.size() + decorators.size());
        localDecorators.addAll(mappedDecorators);

        //  Integer.MAX_VALUE means placing bare unordered decorators after (== inside) mapped decorators
        decorators.forEach(d -> localDecorators.add(new MappedJobDecorator<>(d, Integer.MAX_VALUE)));

        Comparator<MappedJobDecorator> sortOuterToInner = Comparator.comparing(MappedJobDecorator::getOrder);

        List<JobDecorator> decoratorsInnerToOuter = localDecorators.stream()
                // sorting in reverse order, as inner decorators are installed first by JobDecorators
                .sorted(sortOuterToInner.reversed())
                .map(MappedJobDecorator::getDecorator)
                .collect(Collectors.toList());

        // TODO: a second ExceptionsHandlerDecorator in the chain... must unify
        return new JobDecorators(decoratorsInnerToOuter, new ExceptionsHandlerDecorator());
    }

    @Provides
    @Singleton
    MappedJobDecorator<LockHandler> provideMappedLockHandler(LockHandler lockHandler) {
        return new MappedJobDecorator<>(lockHandler, JobDecorators.LOCK_HANDLER_DECORATOR_ORDER);
    }

    @Provides
    @Singleton
    MappedJobDecorator<JobLogger> provideMappedJobLogger(JobLogger jobLogger) {
        return new MappedJobDecorator<>(jobLogger, JobDecorators.LOGGER_DECORATOR_ORDER);
    }

    @Provides
    @Singleton
    MappedJobDecorator<JobListenerDecorator> provideMappedJobLogger(JobListenerDecorator listenerDecorator) {
        return new MappedJobDecorator<>(listenerDecorator, JobDecorators.LISTENERS_DECORATOR_ORDER);
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
    JobListenerDecorator provideListenerDecorator(Set<JobListener> listeners, Set<MappedJobListener> mappedListeners) {
        // not checking for dupes between MappedJobListener and JobListener collections. Is that a problem?
        List<MappedJobListener> localListeners = new ArrayList<>(mappedListeners.size() + listeners.size());
        localListeners.addAll(mappedListeners);

        //  Integer.MAX_VALUE means placing bare unordered listeners after (== inside) mapped listeners
        listeners.forEach(listener -> localListeners.add(new MappedJobListener<>(listener, Integer.MAX_VALUE)));
        localListeners.sort(Comparator.comparing(MappedJobListener::getOrder));

        return new JobListenerDecorator(localListeners);
    }
}
