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

package io.bootique.job.graph;

import io.bootique.BootiqueException;
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobParameterMetadata;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class JobGraphBuilder {

    private final Map<String, JobRef> knownExecutions;
    private final Map<String, Job> standaloneJobs;
    private final JobGraphNodes nodes;

    public JobGraphBuilder(Map<String, JobGraphNode> nodes, Map<String, Job> standaloneJobs) {
        this.nodes = new JobGraphNodes(nodes);
        this.standaloneJobs = standaloneJobs;
        this.knownExecutions = new LinkedHashMap<>();
    }

    /**
     * Creates a dependency graph rooted in the given job name
     */
    public Digraph<JobRef> createGraph(String rootJobName) {
        Digraph<JobRef> graph = new Digraph<>();
        populateWithDependencies(rootJobName, null, graph, nodes, new HashMap<>());
        return graph;
    }

    private void populateWithDependencies(
            String jobName,
            JobRef childRef,
            Digraph<JobRef> graph,
            JobGraphNodes nodes,
            Map<String, JobRef> childRefs) {

        JobGraphNode node = nodes.getNode(jobName);

        node.accept(new JobGraphNodeVisitor() {
            @Override
            public void visitSingle(SingleJobNode singleJob) {
                JobRef ref = getOrCreateRef(jobName, singleJob);
                graph.add(ref);
                if (childRef != null) {
                    graph.add(ref, childRef);
                }
                populateWithSingleJobDependencies(ref, graph, nodes, childRefs);
            }

            @Override
            public void visitGroup(GroupNode group) {
                group.getJobs().forEach((name, definition) -> populateWithDependencies(
                        name,
                        childRef,
                        graph,
                        new JobGraphNodes(group.getJobs(), nodes), childRefs)
                );
            }
        });
    }

    private void populateWithSingleJobDependencies(
            JobRef ref,
            Digraph<JobRef> graph,
            JobGraphNodes nodes,
            Map<String, JobRef> childRefs) {

        String jobName = ref.getJobName();
        childRefs.put(jobName, ref);

        ((SingleJobNode) nodes.getNode(jobName)).getDependsOn().forEach(parentName -> {
            if (childRefs.containsKey(parentName)) {
                String message = String.format("Job dependency cycle detected: [...] -> %s -> %s", jobName, parentName);
                throw new IllegalStateException(message);
            }
            populateWithDependencies(parentName, ref, graph, nodes, childRefs);
        });
        childRefs.remove(jobName);
    }

    private JobRef getOrCreateRef(String jobName, SingleJobNode node) {
        return knownExecutions.computeIfAbsent(jobName, jn -> createRef(jn, node));
    }

    private JobRef createRef(String jobName, SingleJobNode node) {
        Job job = standaloneJobs.get(jobName);

        if (job == null) {
            throw new BootiqueException(1, "No job object for name '" + jobName + "'");
        }

        return new JobRef(jobName, convertParams(job.getMetadata(), node.getParams()));
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

    static class JobGraphNodes {

        private final Map<String, ? extends JobGraphNode> nodes;
        private final JobGraphNodes defaults;

        JobGraphNodes(Map<String, ? extends JobGraphNode> nodes) {
            this(nodes, null);
        }

        JobGraphNodes(Map<String, ? extends JobGraphNode> nodes, JobGraphNodes defaults) {
            this.nodes = Objects.requireNonNull(nodes);
            this.defaults = defaults;
        }

        JobGraphNode getNode(String jobName) {
            JobGraphNode defaultNode = defaults != null ? defaults.getNode(jobName) : null;
            JobGraphNode overrideNode = nodes.get(jobName);

            if (overrideNode instanceof SingleJobNode && defaultNode instanceof SingleJobNode) {
                return ((SingleJobNode) defaultNode).merge((SingleJobNode) overrideNode);
            }

            if (overrideNode == null && defaultNode == null) {
                throw new BootiqueException(1, "No job object for name '" + jobName + "'");
            }

            return overrideNode != null ? overrideNode : defaultNode;
        }
    }
}
