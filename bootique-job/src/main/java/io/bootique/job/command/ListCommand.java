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
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobParameterMetadata;
import io.bootique.job.JobRegistry;
import io.bootique.log.BootLogger;
import io.bootique.meta.application.CommandMetadata;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ListCommand extends CommandWithMetadata {

    private final Provider<JobRegistry> jobRegistryProvider;
    private final BootLogger bootLogger;

    private static CommandMetadata createMetadata() {
        return CommandMetadata.builder(ListCommand.class).description("Lists all jobs available in the app").build();
    }

    @Inject
    public ListCommand(Provider<JobRegistry> jobRegistryProvider, BootLogger bootLogger) {
        super(createMetadata());

        this.jobRegistryProvider = jobRegistryProvider;
        this.bootLogger = bootLogger;
    }

    @Override
    public CommandOutcome run(Cli cli) {

        JobRegistry jobRegistry = jobRegistryProvider.get();
        Collection<JobMetadata> jobsInfo = jobRegistry.getJobNames()
                .stream()
                .map(jobRegistry::getJob)
                .map(Job::getMetadata)
                .sorted(Comparator.comparing(JobMetadata::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        if (jobsInfo.isEmpty()) {
            bootLogger.stdout("No jobs.");
            return CommandOutcome.succeeded();
        }

        bootLogger.stdout("Available jobs:");
        jobsInfo.forEach(this::printJobInfo);
        return CommandOutcome.succeeded();
    }

    private void printJobInfo(JobMetadata md) {
        String paramsString = paramsToString(md.getParameters());
        bootLogger.stdout(String.format("     - %s%s", md.getName(), paramsString));
    }

    String paramsToString(Collection<JobParameterMetadata<?>> params) {
        return params.stream().map(this::paramToString).collect(Collectors.joining(", ", "(", ")"));
    }

    String paramToString(JobParameterMetadata<?> pmd) {
        StringBuilder buffer = new StringBuilder()
                .append(pmd.getName())
                .append(":")
                .append(pmd.getTypeName());

        if (pmd.getDefaultValue() != null) {
            buffer.append("=").append(pmd.getDefaultValue());
        }

        return buffer.toString();
    }
}
