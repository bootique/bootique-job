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

package io.bootique.job.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.meta.application.OptionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ScheduleCommand extends CommandWithMetadata {

    public static final String JOB_OPTION = "job";
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleCommand.class);
    private Provider<Scheduler> schedulerProvider;

    @Inject
    public ScheduleCommand(Provider<Scheduler> schedulerProvider) {
        super(createMetadata());
        this.schedulerProvider = schedulerProvider;
    }

    private static OptionMetadata.Builder createJobOption() {
        return OptionMetadata.builder(JOB_OPTION).description("Specifies the name of the job to schedule. "
                + "Available job names can be viewed using '--list' command.").valueRequired("job_name");
    }

    private static CommandMetadata createMetadata() {
        return CommandMetadata.builder(ScheduleCommand.class)
                .description(
                        "Schedules and executes jobs according to configuration and '--job' arguments. Waits indefinitely on the foreground.")
                .addOption(createJobOption())
                .build();
    }

    @Override
    public CommandOutcome run(Cli cli) {
        Scheduler scheduler = schedulerProvider.get();

        int jobCount;

        List<String> jobNames = cli.optionStrings(JOB_OPTION);
        if (jobNames == null || jobNames.isEmpty()) {
            LOGGER.info("Starting scheduler");
            jobCount = scheduler.start();
        } else {
            LOGGER.info("Starting scheduler for jobs: " + jobNames);
            jobCount = scheduler.start(jobNames);
        }

        LOGGER.info("Started scheduler with {} trigger(s).", jobCount);
        return CommandOutcome.succeededAndForkedToBackground();
    }

}
