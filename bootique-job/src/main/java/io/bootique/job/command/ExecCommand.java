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
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.JobModule;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.meta.application.OptionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.stream.Collectors;

public class ExecCommand extends CommandWithMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecCommand.class);

    public static final String SERIAL_OPTION = "serial";

    private final Provider<Scheduler> schedulerProvider;

    private static OptionMetadata.Builder createSerialOption() {
        return OptionMetadata.builder(SERIAL_OPTION).description("Enforces sequential execution of the jobs, " +
                "specified with '--job' options.");
    }

    private static CommandMetadata createMetadata() {
        return CommandMetadata.builder(ExecCommand.class)
                .description("Executes one or more jobs. Jobs are specified with '--job' options")
                .addOption(createSerialOption()).build();
    }

    // using Provider for lazy init
    @Inject
    public ExecCommand(Provider<Scheduler> schedulerProvider) {
        super(createMetadata());
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public CommandOutcome run(Cli cli) {

        List<String> jobNames = cli.optionStrings(JobModule.JOB_OPTION);
        if (jobNames == null || jobNames.isEmpty()) {
            return CommandOutcome.failed(1,
                    String.format("No jobs specified. Use '--%s' option to provide job names", JobModule.JOB_OPTION));
        }

        LOGGER.info("Will run job(s): {}", jobNames);

        return cli.hasOption(SERIAL_OPTION)
                ? runSerial(jobNames, schedulerProvider.get())
                : runParallel(jobNames, schedulerProvider.get());
    }

    private CommandOutcome runParallel(List<String> jobNames, Scheduler scheduler) {

        // to ensure parallel execution, must collect futures in an explicit collection,
        // and then "get" them in a separate stream
        List<JobFuture> futures = jobNames.stream().map(scheduler::runOnce).collect(Collectors.toList());

        String failed = futures.stream()
                .map(JobFuture::get)
                .peek(this::processResult)
                .filter(result -> !result.isSuccess())
                .map(r -> r.getMetadata().getName())
                .collect(Collectors.joining(", "));

        return failed.isEmpty() ? CommandOutcome.succeeded() : CommandOutcome.failed(1, "Some of the jobs failed: " + failed);
    }

    private CommandOutcome runSerial(List<String> jobNames, Scheduler scheduler) {
        for (String jobName : jobNames) {
            JobResult result = scheduler.runOnce(jobName).get();
            processResult(result);
            if (!result.isSuccess()) {
                return CommandOutcome.failed(1, "One of the jobs failed: " + jobName);
            }
        }
        return CommandOutcome.succeeded();
    }

    private void processResult(JobResult result) {
        String message = String.format("Finished job '%s', result: %s, message: %s",
                result.getMetadata().getName(),
                result.getOutcome(),
                result.getMessage());

        if (result.getThrowable() == null) {
            LOGGER.info(message);
        } else {
            LOGGER.error(message, result.getThrowable());
        }
    }
}
