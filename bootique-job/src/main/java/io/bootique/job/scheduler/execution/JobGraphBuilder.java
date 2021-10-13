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

package io.bootique.job.scheduler.execution;

import io.bootique.BootiqueException;
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobParameterMetadata;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.JobGroupDefinition;
import io.bootique.job.config.SingleJobDefinition;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class JobGraphBuilder {

    private final Map<String, JobExecution> knownExecutions;
    private final Map<String, Job> standaloneJobs;
    private final JobDefinitions definitions;

    JobGraphBuilder(Map<String, JobDefinition> definitions, Map<String, Job> standaloneJobs) {
        this.definitions = new JobDefinitions(definitions);
        this.standaloneJobs = standaloneJobs;
        this.knownExecutions = new LinkedHashMap<>();
    }

    /**
     * Creates a dependency graph rooted in the given job name
     */
    public DIGraph<JobExecution> createGraph(String rootJobName) {
        DIGraph<JobExecution> graph = new DIGraph<>();
        populateWithDependencies(rootJobName, null, graph, definitions, new HashMap<>());
        return graph;
    }

    private void populateWithDependencies(
            String jobName,
            JobExecution childExecution,
            DIGraph<JobExecution> graph,
            JobDefinitions jobDefinitions,
            Map<String, JobExecution> childExecutions) {

        JobDefinition jobDefinition = jobDefinitions.getDefinition(jobName);
        if (jobDefinition instanceof SingleJobDefinition) {
            SingleJobDefinition singleJob = (SingleJobDefinition) jobDefinition;
            JobExecution execution = getOrCreateExecution(jobName, singleJob);
            graph.add(execution);
            if (childExecution != null) {
                graph.add(execution, childExecution);
            }
            populateWithSingleJobDependencies(execution, graph, jobDefinitions, childExecutions);

        } else if (jobDefinition instanceof JobGroupDefinition) {
            JobGroupDefinition group = (JobGroupDefinition) jobDefinition;
            group.getJobs().forEach((name, definition) -> {
                JobDefinitions groupDefinitions = new JobDefinitions(group.getJobs(), jobDefinitions);
                populateWithDependencies(name, childExecution, graph, groupDefinitions, childExecutions);
            });

        } else {
            throw createUnexpectedJobDefinitionError(jobDefinition);
        }
    }

    private void populateWithSingleJobDependencies(
            JobExecution execution,
            DIGraph<JobExecution> graph,
            JobDefinitions jobDefinitions,
            Map<String, JobExecution> childExecutions) {

        String jobName = execution.getJobName();
        childExecutions.put(jobName, execution);
        ((SingleJobDefinition) jobDefinitions.getDefinition(jobName)).getDependsOn().ifPresent(parents ->
                parents.forEach(parentName -> {
                    if (childExecutions.containsKey(parentName)) {
                        throw new IllegalStateException(String.format("Cycle: [...] -> %s -> %s", jobName, parentName));
                    }
                    populateWithDependencies(parentName, execution, graph, jobDefinitions, childExecutions);
                }));
        childExecutions.remove(jobName);
    }

    private JobExecution getOrCreateExecution(String jobName, SingleJobDefinition definition) {

        return knownExecutions.computeIfAbsent(jobName, jn -> {
            Job job = standaloneJobs.get(jobName);

            if (job == null) {
                throw new BootiqueException(1, "No job object for name '" + jobName + "'");
            }

            return new JobExecution(jobName, convertParams(job.getMetadata(), definition.getParams()));
        });
    }

    private Map<String, Object> convertParams(JobMetadata jobMD, Map<String, String> params) {
        // clone params map in order to preserve parameters that were not specified in metadata
        Map<String, Object> convertedParams = new HashMap<>(params);
        for (JobParameterMetadata<?> param : jobMD.getParameters()) {
            String valueString = params.get(param.getName());
            Object value = param.fromString(valueString);
            convertedParams.put(param.getName(), value);
        }
        return convertedParams;
    }

    private RuntimeException createUnexpectedJobDefinitionError(JobDefinition definition) {
        return new IllegalArgumentException("Unexpected job definition type: " + definition.getClass().getName());
    }
}
