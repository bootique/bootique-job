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

import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.job.JobModule;
import io.bootique.job.Scheduler;
import io.bootique.job.trigger.JobExec;
import io.bootique.job.trigger.JobExecParser;
import io.bootique.meta.application.CommandMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.stream.Collectors;


public class ScheduleCommand extends CommandWithMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleCommand.class);

    private final Provider<JobExecParser> jobExecParser;
    private Provider<Scheduler> schedulerProvider;

    @Inject
    public ScheduleCommand(Provider<JobExecParser> jobExecParser, Provider<Scheduler> schedulerProvider) {
        super(createMetadata());
        this.jobExecParser = jobExecParser;
        this.schedulerProvider = schedulerProvider;
    }

    private static CommandMetadata createMetadata() {
        return CommandMetadata.builder(ScheduleCommand.class)
                .description("Starts a job scheduler that will execute job(s) periodically according to configuration and an optional '--job' arguments.")
                .build();
    }

    @Override
    public CommandOutcome run(Cli cli) {

        int jobCount;
        Scheduler scheduler = schedulerProvider.get();

        List<String> jobStrings = cli.optionStrings(JobModule.JOB_OPTION);
        if (jobStrings == null || jobStrings.isEmpty()) {
            LOGGER.info("Starting scheduler");
            jobCount = scheduler.start();
        } else {
            
            JobExecParser parser = jobExecParser.get();
            List<String> jobNames = jobStrings.stream()
                    .map(parser::parse)
                    // TODO: allow parameters to be passed to the scheduler the same way we allow it for individual
                    //  executions
                    .peek(e -> {
                        if (!e.getParams().isEmpty()) {
                            LOGGER.warn("Ignoring CLI parameters of job {} for scheduling: ", e.getJobName(), e.getParams());
                        }
                    })
                    .map(JobExec::getJobName)
                    .collect(Collectors.toList());

            LOGGER.info("Starting scheduler for jobs: {}", jobNames);
            jobCount = scheduler.start(jobNames);
        }

        LOGGER.info("Started scheduler with {} trigger(s).", jobCount);
        return CommandOutcome.succeededAndForkedToBackground();
    }

}
