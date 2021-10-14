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

import io.bootique.BQCoreModule;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.help.ValueObjectDescriptor;
import io.bootique.job.JobRegistry;
import io.bootique.job.command.ExecCommand;
import io.bootique.job.command.ListCommand;
import io.bootique.job.command.ScheduleCommand;
import io.bootique.job.lock.LocalLockHandler;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.scheduler.SchedulerFactory;
import io.bootique.job.value.Cron;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.shutdown.ShutdownManager;

import javax.inject.Singleton;

public class JobModule extends ConfigModule {

    static final String JOBS_CONFIG_PREFIX = "jobs";
    static final String SCHEDULER_CONFIG_PREFIX = "scheduler";

    public static final String JOB_OPTION = "job";


    /**
     * @deprecated since 3.0 as JobMDCManager is no longer implemented as a listener.
     */
    // TX ID listener is usually the outermost listener in any app. It is a good idea to order your other listeners
    // relative to this one , using higher ordering values.
    @Deprecated
    public static final int BUSINESS_TX_LISTENER_ORDER = Integer.MIN_VALUE + 800;

    /**
     * @deprecated since 3.0 as JobLogListener is no longer exists
     */
    // goes inside LOG_LISTENER_ORDER
    @Deprecated
    public static final int LOG_LISTENER_ORDER = BUSINESS_TX_LISTENER_ORDER + 200;

    private final LockHandler defaultLockHandler = new LocalLockHandler();

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
                .addValueObjectDescriptor(Cron.class, new ValueObjectDescriptor("percent expression, e.g. '0 0 * * * *'"));

        JobModule.extend(binder)
                .initAllExtensions()
                .setLockHandler(defaultLockHandler);
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
}
