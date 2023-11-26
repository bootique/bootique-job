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
import io.bootique.job.*;
import io.bootique.job.trigger.JobExec;
import io.bootique.job.trigger.JobExecParser;
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

    private final Provider<JobExecParser> jobExecParser;
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

    @Inject
    public ExecCommand(Provider<JobExecParser> jobExecParser, Provider<Scheduler> schedulerProvider) {
        super(createMetadata());
        this.jobExecParser = jobExecParser;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public CommandOutcome run(Cli cli) {

        List<String> jobStrings = cli.optionStrings(SchedulerModule.JOB_OPTION);
        if (jobStrings == null || jobStrings.isEmpty()) {
            return CommandOutcome.failed(1,
                    String.format("No jobs specified. Use '--%s' option to provide job names", SchedulerModule.JOB_OPTION));
        }

        LOGGER.info("Will run job(s): {}", jobStrings);

        JobExecParser parser = jobExecParser.get();
        List<JobExec> executions = jobStrings.stream().map(parser::parse).collect(Collectors.toList());

        Scheduler scheduler = schedulerProvider.get();

        return cli.hasOption(SERIAL_OPTION) || executions.size() == 1
                ? runSerial(executions, scheduler)
                : runParallel(executions, scheduler);
    }

    private CommandOutcome runParallel(List<JobExec> execs, Scheduler scheduler) {

        // to ensure parallel execution, must collect futures in an explicit collection,
        // and then "get" them in a separate stream
        List<JobFuture> futures = execs.stream()
                .map(e -> scheduler.runBuilder().jobName(e.getJobName()).params(e.getParams()).runNonBlocking())
                .collect(Collectors.toList());

        String failed = futures.stream()
                .map(JobFuture::get)
                .peek(this::processResult)
                .filter(result -> !result.isSuccess())
                .map(r -> r.getMetadata().getName())
                .collect(Collectors.joining(", "));

        return failed.isEmpty() ? CommandOutcome.succeeded() : CommandOutcome.failed(1, "Some of the jobs failed: " + failed);
    }

    private CommandOutcome runSerial(List<JobExec> execs, Scheduler scheduler) {
        for (JobExec e : execs) {
            JobResult result = scheduler.runBuilder().jobName(e.getJobName()).params(e.getParams()).runBlocking();
            processResult(result);
            if (!result.isSuccess()) {
                return CommandOutcome.failed(1, "One of the jobs failed: " + e.getJobName());
            }
        }
        return CommandOutcome.succeeded();
    }

    private void processResult(JobResult result) {
        String message = result.getMessage() != null

                ? String.format("Finished job '%s', result: %s, message: %s",
                result.getMetadata().getName(),
                result.getOutcome(),
                result.getMessage())

                : String.format("Finished job '%s', result: %s",
                result.getMetadata().getName(),
                result.getOutcome());

        if (result.getThrowable() == null) {
            LOGGER.info(message);
        } else {
            LOGGER.error(message, result.getThrowable());
        }
    }
}
