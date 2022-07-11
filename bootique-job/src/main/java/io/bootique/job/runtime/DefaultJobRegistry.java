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
import io.bootique.job.Scheduler;
import io.bootique.job.graph.*;
import io.bootique.job.group.JobGroup;
import io.bootique.job.group.JobGroupStep;
import io.bootique.job.group.ParallelJobBatchStep;
import io.bootique.job.group.SingleJobStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class DefaultJobRegistry implements JobRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJobRegistry.class);

    protected final Provider<Scheduler> scheduler;
    protected final Map<String, Job> standaloneJobs;
    protected final Map<String, JobGraphNode> allNodes;
    protected final JobDecorators decorators;

    // Lazily populated map of decorated runnable jobs (either standalone or groups)
    private final ConcurrentMap<String, Job> decoratedJobAndGroups;

    public DefaultJobRegistry(
            Map<String, Job> standaloneJobs,
            Map<String, JobGraphNode> nodes,
            Provider<Scheduler> scheduler,
            JobDecorators decorators) {

        this.standaloneJobs = standaloneJobs;
        this.allNodes = nodes;
        this.decoratedJobAndGroups = new ConcurrentHashMap<>((int) (nodes.size() / 0.75d) + 1);
        this.scheduler = scheduler;
        this.decorators = decorators;
    }

    @Override
    public Set<String> getJobNames() {
        return allNodes.keySet();
    }

    @Override
    public Job getJob(String jobName) {
        return decoratedJobAndGroups.computeIfAbsent(jobName, this::createJob);
    }

    /**
     * @since 3.0
     */
    protected Job createJob(String jobName) {

        checkJobExists(jobName);

        Digraph<JobNode> graph = new JobGraphBuilder(allNodes).createGraph(jobName);
        List<Job> standaloneJobsInGraph = standaloneJobsInGraph(graph);

        switch (standaloneJobsInGraph.size()) {
            case 1:
                JobNode node = graph.topSort().get(0).iterator().next();
                return decorators.decorateTopJob(node.getJob(), jobName, node.getParams());
            case 0:
                // fall through to the JobGroup
                LOGGER.warn("Job group '{}' is empty. It is valid, but will do nothing", jobName);
            default:
                Job group = createJobGroup(jobName, graph);
                return decorators.decorateTopJob(group, jobName, Collections.emptyMap());
        }
    }

    protected JobGroup createJobGroup(String jobName, Digraph<JobNode> graph) {
        List<Set<JobNode>> sortedNodes = graph.reverseTopSort();
        return createJobGroup(
                groupMetadata(jobName, allNodes.get(jobName), sortedNodes),
                jobGroupSteps(sortedNodes));
    }

    protected JobGroup createJobGroup(JobMetadata groupMetadata, List<JobGroupStep> steps) {
        return new JobGroup(groupMetadata, steps);
    }

    private JobMetadata groupMetadata(String groupName, JobGraphNode groupConfig, List<Set<JobNode>> nodes) {

        JobMetadata.Builder builder = JobMetadata
                .builder(groupName)
                .dependsOn(groupConfig.getDependsOn())
                .group(true);

        // TODO: While we do need to capture parameters (especially for single job groups), is it correct for the
        //  group parameters to be the union of child job params? What if there are conflicting names?

        for (Set<JobNode> nodeSet : nodes) {
            for (JobNode node : nodeSet) {
                node.getJob().getMetadata().getParameters().forEach(builder::param);
            }
        }

        return builder.build();
    }

    protected List<JobGroupStep> jobGroupSteps(List<Set<JobNode>> sortedNodes) {

        List<JobGroupStep> steps = new ArrayList<>(sortedNodes.size());

        for (Set<JobNode> s : sortedNodes) {
            List<Job> stepJobs = new ArrayList<>();
            for (JobNode e : s) {

                Job undecorated = standaloneJobs.get(e.getName());
                Job decorated = decorators.decorateSubJob(undecorated, null, e.getParams());
                stepJobs.add(decorated);
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
        return new SingleJobStep(scheduler.get(), job);
    }

    protected ParallelJobBatchStep createParallelGroupStep(List<Job> stepJobs) {
        return new ParallelJobBatchStep(scheduler.get(), stepJobs);
    }

    private List<Job> standaloneJobsInGraph(Digraph<JobNode> graph) {
        return graph.allVertices().stream()
                .map(JobNode::getJob)
                .collect(Collectors.toList());
    }

    private void checkJobExists(String jobName) {
        if (!allNodes.containsKey(jobName)) {
            throw new IllegalArgumentException("Unknown job: " + jobName);
        }
    }
}
