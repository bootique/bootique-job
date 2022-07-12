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

package io.bootique.job.runtime;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobRegistry;
import io.bootique.job.graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultJobRegistry implements JobRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJobRegistry.class);

    protected final Map<String, JobGraphNode> jobDescriptors;
    protected final JobDecorators decorators;

    // GraphExecutor internally keeps a thread pool. Initialize it lazily, so that it won't start for the
    // apps that don't have jobs with dependencies
    protected final Provider<GraphExecutor> graphExecutor;

    // Lazily populated map of decorated runnable jobs (jobs or graphs) produced from standalone jobs and groups
    private final ConcurrentMap<String, Job> executableJobs;

    public DefaultJobRegistry(
            Map<String, JobGraphNode> nodes,
            JobDecorators decorators,
            Provider<GraphExecutor> graphExecutor) {

        this.jobDescriptors = nodes;
        this.executableJobs = new ConcurrentHashMap<>((int) (nodes.size() / 0.75d) + 1);
        this.decorators = decorators;
        this.graphExecutor = graphExecutor;
    }

    @Override
    public Set<String> getJobNames() {
        return jobDescriptors.keySet();
    }

    @Override
    public Job getJob(String jobName) {
        return executableJobs.computeIfAbsent(jobName, this::createExecutableJob);
    }

    /**
     * @since 3.0
     */
    protected Job createExecutableJob(String name) {

        checkJobExists(name);

        JobNodeDigraph graph = JobNodeDigraph.builder(jobDescriptors).create(name);

        switch (graph.verticesCount()) {
            case 1:
                JobNode node = graph.topSort().get(0).iterator().next();
                return decorators.decorateTopJob(node.getJob(), name, node.getParams());
            case 0:
                // fall through to the JobGroup
                LOGGER.warn("Job group '{}' is empty. It is valid, but will do nothing", name);
            default:
                Job group = createExecutableGraph(name, graph);
                return decorators.decorateTopJob(group, name, Collections.emptyMap());
        }
    }

    protected GraphJob createExecutableGraph(String name, Digraph<JobNode> graph) {
        List<Set<JobNode>> sortedNodes = graph.reverseTopSort();

        return new GraphJob(
                createGraphMetadata(jobDescriptors.get(name), sortedNodes),
                jobGroupSteps(sortedNodes));
    }

    private JobMetadata createGraphMetadata(JobGraphNode graphNode, List<Set<JobNode>> executionNodes) {

        JobMetadata.Builder builder = JobMetadata
                .builder(graphNode.getName())
                .dependsOn(graphNode.getDependsOn())
                .group(true);

        // TODO: While we do need to capture parameters (especially for single job groups), is it correct for the
        //  group parameters to be the union of child job params? What if there are conflicting names?

        for (Set<JobNode> nodeSet : executionNodes) {
            for (JobNode node : nodeSet) {
                node.getJob().getMetadata().getParameters().forEach(builder::param);
            }
        }

        return builder.build();
    }

    protected List<GraphJobStep> jobGroupSteps(List<Set<JobNode>> sortedNodes) {

        List<GraphJobStep> steps = new ArrayList<>(sortedNodes.size());

        for (Set<JobNode> s : sortedNodes) {
            List<Job> stepJobs = new ArrayList<>();
            for (JobNode e : s) {

                JobGraphNode node = jobDescriptors.get(e.getName());

                node.accept(new JobGraphNodeVisitor() {
                    @Override
                    public void visitJob(JobNode jobNode) {
                        Job decorated = decorators.decorateSubJob(jobNode.getJob(), null, e.getParams());
                        stepJobs.add(decorated);
                    }

                    @Override
                    public void visitGroup(GroupNode groupNode) {
                        // TODO: no reason we can't support groups as children of jobs or other groups
                        throw new IllegalStateException("Don't (yet) support groups as children on the job dependency tree: " + groupNode.getName());
                    }
                });
            }

            switch (stepJobs.size()) {
                case 0:
                    break;
                case 1:
                    steps.add(createSingleJobStep(stepJobs.get(0)));
                    break;
                default:
                    steps.add(createParallelGroupStep(stepJobs));
                    break;
            }
        }

        return steps;
    }

    protected SingleJobStep createSingleJobStep(Job job) {
        return new SingleJobStep(job);
    }

    protected ParallelJobsStep createParallelGroupStep(List<Job> stepJobs) {
        return new ParallelJobsStep(graphExecutor.get(), stepJobs);
    }

    private void checkJobExists(String jobName) {
        if (!jobDescriptors.containsKey(jobName)) {
            throw new IllegalArgumentException("Unknown job: " + jobName);
        }
    }
}
