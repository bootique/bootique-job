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
    protected final Set<String> allJobsAndGroupNames;
    protected final JobDecorators decorators;

    // Lazily populated map of decorated runnable jobs (either standalone or groups)
    private final ConcurrentMap<String, Job> decoratedJobAndGroups;

    public DefaultJobRegistry(
            Collection<Job> standaloneJobs,
            Map<String, JobGraphNode> nodes,
            Provider<Scheduler> scheduler,
            JobDecorators decorators) {

        this.standaloneJobs = jobsByName(standaloneJobs);
        this.allJobsAndGroupNames = allJobsAndGroupNames(this.standaloneJobs, nodes);
        this.allNodes = allNodes(this.standaloneJobs.keySet(), nodes);
        this.decoratedJobAndGroups = new ConcurrentHashMap<>((int) (nodes.size() / 0.75d) + 1);
        this.scheduler = scheduler;
        this.decorators = decorators;
    }

    private Set<String> allJobsAndGroupNames(
            Map<String, Job> standaloneJobs,
            Map<String, JobGraphNode> jobDefinitions) {

        // TODO: how do we check for conflicts between standalone job names and group names?
        Set<String> jobNames = new HashSet<>(standaloneJobs.keySet());
        jobNames.addAll(jobDefinitions.keySet());
        return jobNames;
    }

    private Map<String, JobGraphNode> allNodes(
            Set<String> standaloneJobsNames,
            Map<String, JobGraphNode> configured) {

        // combine explicit job nodes from config with default nodes for the existing jobs
        Map<String, JobGraphNode> combined = new HashMap<>(configured);

        // create a node for each job, that is not explicitly present in the config
        standaloneJobsNames.stream()
                .filter(n -> !combined.containsKey(n))
                .forEach(n -> combined.put(n, new SingleJobNode()));

        return combined;
    }

    @Override
    public Set<String> getJobNames() {
        return allJobsAndGroupNames;
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

        Digraph<JobRef> graph = new JobGraphBuilder(allNodes, standaloneJobs).createGraph(jobName);
        List<Job> standaloneJobsInGraph = standaloneJobsInGraph(graph);

        switch (standaloneJobsInGraph.size()) {
            case 1:
                JobRef ref = graph.topSort().get(0).iterator().next();
                Job job = standaloneJobsInGraph.get(0);
                return decorators.decorateStandaloneJob(job, jobName, ref.getParams());
            case 0:
                // fall through to the JobGroup
                LOGGER.warn("Job group '{}' is empty. It is valid, but will do nothing", jobName);
            default:
                Job group = createJobGroup(jobName, graph);
                return decorators.decorateStandaloneJob(group, jobName, Collections.emptyMap());
        }
    }

    protected JobGroup createJobGroup(String jobName, Digraph<JobRef> graph) {
        JobMetadata groupMetadata = groupMetadata(jobName, standaloneJobs.values());
        List<JobGroupStep> steps = jobGroupSteps(graph.reverseTopSort());
        return createJobGroup(groupMetadata, steps);
    }

    protected JobGroup createJobGroup(JobMetadata groupMetadata, List<JobGroupStep> steps) {
        return new JobGroup(groupMetadata, steps);
    }

    private JobMetadata groupMetadata(String jobName, Collection<Job> jobs) {
        JobMetadata.Builder builder = JobMetadata.builder(jobName);
        for (Job job : jobs) {
            job.getMetadata().getParameters().forEach(builder::param);
        }
        return builder.build();
    }

    protected List<JobGroupStep> jobGroupSteps(List<Set<JobRef>> sortedRefs) {

        List<JobGroupStep> steps = new ArrayList<>(sortedRefs.size());

        for (Set<JobRef> s : sortedRefs) {
            List<Job> stepJobs = new ArrayList<>();
            for (JobRef e : s) {

                Job undecorated = standaloneJobs.get(e.getJobName());
                Job decorated = decorators.decorateGroupMemberJob(undecorated, null, e.getParams());
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

    private Map<String, Job> jobsByName(Collection<Job> jobs) {

        // report job name conflicts, but otherwise ignore them
        // TODO: should we throw?

        Map<String, Job> map = new HashMap<>();
        for (Job j : jobs) {

            String name = j.getMetadata().getName();
            Job existing = map.put(name, j);
            if (existing != null && existing != j) {
                LOGGER.warn("Duplicate job name '{}' was ignored and one of the jobs discarded", name);
            }
        }

        return map;
    }

    private List<Job> standaloneJobsInGraph(Digraph<JobRef> graph) {
        return graph.allVertices().stream()
                .map(e -> standaloneJobs.get(e.getJobName()))
                .filter(j -> j != null)
                .collect(Collectors.toList());
    }

    private void checkJobExists(String jobName) {
        if (!allJobsAndGroupNames.contains(jobName)) {
            throw new IllegalArgumentException("Unknown job: " + jobName);
        }
    }
}