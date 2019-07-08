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

package io.bootique.job.runtime;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.BQCoreModule;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.help.ValueObjectDescriptor;
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
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.scheduler.SchedulerFactory;
import io.bootique.job.scheduler.execution.DefaultJobRegistry;
import io.bootique.job.value.Cron;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.shutdown.ShutdownManager;
import io.bootique.type.TypeRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JobModule extends ConfigModule {

    public static final String JOB_OPTION = "job";

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
                .addOption(OptionMetadata.builder(JOB_OPTION).description("Specifies the name of the job to execute or schedule. "
                        + "Used in conjunction with '--execute' or '--schedule' commands. "
                        + "Available job names can be viewed using '--list' command.")
                        .valueRequired("job_name")
                        .build())
                .addCommand(ListCommand.class)
                .addCommand(ScheduleCommand.class)
                .addValueObjectDescriptor(Cron.class, new ValueObjectDescriptor("percent expression, e.g. '0 0 * * * *'"));

        JobModule.extend(binder)
                .initAllExtensions()
                .addMappedListener(new TypeLiteral<MappedJobListener<JobLogListener>>() {
                });
    }

    @Provides
    @Singleton
    LockHandler provideLocalLockHandler() {
        return new LocalLockHandler();
    }

    @Provides
    @Singleton
    Scheduler createScheduler(
            LockHandler serialJobRunner,
            JobRegistry jobRegistry,
            ConfigurationFactory configFactory,
            ShutdownManager shutdownManager) {

        return config(SchedulerFactory.class, configFactory)
                .createScheduler(serialJobRunner, jobRegistry, shutdownManager);
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

        localListeners.sort(Comparator.comparing(MappedJobListener::getOrder));

        return localListeners;
    }
}
