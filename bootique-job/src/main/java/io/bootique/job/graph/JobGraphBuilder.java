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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class JobGraphBuilder {

    private final JobGraphNodes nodes;
    private final Map<String, JobNode> seenNodes;

    public JobGraphBuilder(Map<String, JobGraphNode> nodes) {
        this.nodes = new JobGraphNodes(nodes);
        this.seenNodes = new LinkedHashMap<>();
    }

    /**
     * Creates a dependency graph rooted in the given job name
     */
    public Digraph<JobNode> createGraph(String rootJobName) {

        // build a graph that contains relationships between JobNodes (skipping group nodes that are not executable)

        Digraph<JobNode> graph = new Digraph<>();
        populateWithDependencies(rootJobName, null, graph, nodes, new HashMap<>());
        return graph;
    }

    private void populateWithDependencies(
            String jobName,
            JobNode childRef,
            Digraph<JobNode> graph,
            JobGraphNodes nodes,
            Map<String, JobNode> childRefs) {

        JobGraphNode node = nodes.getNode(jobName);

        node.accept(new JobGraphNodeVisitor() {
            @Override
            public void visitJob(JobNode jobNode) {

                JobNode uniqueNode = uniqueNode(jobNode);

                graph.add(uniqueNode);
                if (childRef != null) {
                    graph.add(uniqueNode, childRef);
                }
                populateWithSingleJobDependencies(uniqueNode, graph, nodes, childRefs);
            }

            @Override
            public void visitGroup(GroupNode group) {
                group.getChildren().forEach((name, definition) -> populateWithDependencies(
                        name,
                        childRef,
                        graph,
                        new JobGraphNodes(group.getChildren(), nodes), childRefs)
                );
            }
        });
    }

    private void populateWithSingleJobDependencies(
            JobNode node,
            Digraph<JobNode> graph,
            JobGraphNodes nodes,
            Map<String, JobNode> childNodes) {

        String jobName = node.getName();
        childNodes.put(jobName, node);

        nodes.getNode(jobName).getDependsOn().forEach(parentName -> {
            if (childNodes.containsKey(parentName)) {
                String message = String.format("Job dependency cycle detected: [...] -> %s -> %s", jobName, parentName);
                throw new IllegalStateException(message);
            }
            populateWithDependencies(parentName, node, graph, nodes, childNodes);
        });
        childNodes.remove(jobName);
    }

    private JobNode uniqueNode(JobNode node) {
        // TODO: what if the same job name is used twice in the graph, but with different params and dependencies?
        //  It is probably incorrect to collapse it to a single exec node
        return seenNodes.computeIfAbsent(node.getName(), name -> node);
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

            if (overrideNode instanceof JobNode && defaultNode instanceof JobNode) {
                return ((JobNode) defaultNode).merge((JobNode) overrideNode);
            }

            if (overrideNode == null && defaultNode == null) {
                throw new BootiqueException(1, "No job object for name '" + jobName + "'");
            }

            return overrideNode != null ? overrideNode : defaultNode;
        }
    }
}
