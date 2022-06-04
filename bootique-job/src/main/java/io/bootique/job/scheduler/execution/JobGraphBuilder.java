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
import io.bootique.job.graph.JobGraphNode;
import io.bootique.job.graph.JobGraphNodeVisitor;
import io.bootique.job.graph.GroupNode;
import io.bootique.job.graph.SingleJobNode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class JobGraphBuilder {

    private final Map<String, JobExecution> knownExecutions;
    private final Map<String, Job> standaloneJobs;
    private final JobGraphNodes nodes;

    JobGraphBuilder(Map<String, JobGraphNode> nodes, Map<String, Job> standaloneJobs) {
        this.nodes = new JobGraphNodes(nodes);
        this.standaloneJobs = standaloneJobs;
        this.knownExecutions = new LinkedHashMap<>();
    }

    /**
     * Creates a dependency graph rooted in the given job name
     */
    public DIGraph<JobExecution> createGraph(String rootJobName) {
        DIGraph<JobExecution> graph = new DIGraph<>();
        populateWithDependencies(rootJobName, null, graph, nodes, new HashMap<>());
        return graph;
    }

    private void populateWithDependencies(
            String jobName,
            JobExecution childExecution,
            DIGraph<JobExecution> graph,
            JobGraphNodes nodes,
            Map<String, JobExecution> childExecutions) {

        JobGraphNode node = nodes.getNode(jobName);

        node.accept(new JobGraphNodeVisitor() {
            @Override
            public void visitSingle(SingleJobNode singleJob) {
                JobExecution execution = getOrCreateExecution(jobName, singleJob);
                graph.add(execution);
                if (childExecution != null) {
                    graph.add(execution, childExecution);
                }
                populateWithSingleJobDependencies(execution, graph, nodes, childExecutions);
            }

            @Override
            public void visitGroup(GroupNode group) {
                group.getJobs().forEach((name, definition) -> populateWithDependencies(
                        name,
                        childExecution,
                        graph,
                        new JobGraphNodes(group.getJobs(), nodes), childExecutions)
                );
            }
        });
    }

    private void populateWithSingleJobDependencies(
            JobExecution execution,
            DIGraph<JobExecution> graph,
            JobGraphNodes nodes,
            Map<String, JobExecution> childExecutions) {

        String jobName = execution.getJobName();
        childExecutions.put(jobName, execution);

        ((SingleJobNode) nodes.getNode(jobName)).getDependsOn().forEach(parentName -> {
            if (childExecutions.containsKey(parentName)) {
                throw new IllegalStateException(String.format("Cycle: [...] -> %s -> %s", jobName, parentName));
            }
            populateWithDependencies(parentName, execution, graph, nodes, childExecutions);
        });
        childExecutions.remove(jobName);
    }

    private JobExecution getOrCreateExecution(String jobName, SingleJobNode definition) {
        return knownExecutions.computeIfAbsent(jobName, jn -> createExecution(jn, definition));
    }

    private JobExecution createExecution(String jobName, SingleJobNode definition) {
        Job job = standaloneJobs.get(jobName);

        if (job == null) {
            throw new BootiqueException(1, "No job object for name '" + jobName + "'");
        }

        return new JobExecution(jobName, convertParams(job.getMetadata(), definition.getParams()));
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
}
