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
import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Injector;
import io.bootique.di.Provides;
import io.bootique.help.ValueObjectDescriptor;
import io.bootique.jackson.JacksonService;
import io.bootique.job.command.ExecCommand;
import io.bootique.job.command.ListCommand;
import io.bootique.job.command.ScheduleCommand;
import io.bootique.job.graph.JobGraphNode;
import io.bootique.job.runtime.DefaultJobRegistry;
import io.bootique.job.runtime.GraphExecutor;
import io.bootique.job.runtime.JobDecorators;
import io.bootique.job.scheduler.SchedulerFactory;
import io.bootique.job.trigger.JobExecParser;
import io.bootique.job.value.Cron;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.shutdown.ShutdownManager;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;

/**
 * @since 3.0
 */
public class SchedulerModule implements BQModule {

    private static final String CONFIG_PREFIX = "scheduler";
    public static final String JOB_OPTION = "job";

    private static OptionMetadata jobOption() {
        return OptionMetadata.builder(JOB_OPTION).description("Specifies the name of a job to execute or schedule. "
                        + "Job name may be optionally followed by a JSON map containing job parameters (e.g. 'myjob{\"p\":1}') "
                        + "Used in conjunction with '--execute' or '--schedule' commands. "
                        + "Available job names can be viewed with '--list' command.")
                .valueRequired("job_name")
                .build();
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Provides Bootique's own job execution engine")
                .config(CONFIG_PREFIX, SchedulerFactory.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {
        BQCoreModule.extend(binder)
                .addOption(jobOption())
                .addCommand(ExecCommand.class)
                .addCommand(ListCommand.class)
                .addCommand(ScheduleCommand.class)
                .addValueObjectDescriptor(Cron.class, new ValueObjectDescriptor("6-part cron expression, e.g. '0 0 * * * *'"));
    }

    @Provides
    @Singleton
    JobExecParser provideExecParser(JobRegistry registry, JacksonService jackson) {
        return new JobExecParser(registry, jackson.newObjectMapper());
    }

    @Provides
    @Singleton
    Scheduler provideScheduler(
            JobRegistry jobRegistry,
            JobDecorators decorators,
            ConfigurationFactory configFactory,
            ShutdownManager shutdownManager) {

        return configFactory
                .config(SchedulerFactory.class, CONFIG_PREFIX)
                .createScheduler(jobRegistry, decorators, shutdownManager);
    }

    @Provides
    @Singleton
    JobRegistry provideJobRegistry(
            Map<String, JobGraphNode> jobNodes,
            JobDecorators decorators,
            Provider<GraphExecutor> graphExecutor) {

        return new DefaultJobRegistry(
                jobNodes,
                decorators,
                graphExecutor);
    }

    // this is a secondary thread pool used for graph execution
    @Provides
    @Singleton
    GraphExecutor provideGraphExecutor(
            ConfigurationFactory configFactory,
            Injector injector,
            ShutdownManager shutdownManager) {
        return configFactory.config(SchedulerFactory.class, CONFIG_PREFIX).createGraphExecutor(injector, shutdownManager);
    }
}
