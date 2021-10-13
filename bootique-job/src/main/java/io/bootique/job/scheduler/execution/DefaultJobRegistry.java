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

import io.bootique.job.*;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.SingleJobDefinition;
import io.bootique.job.scheduler.Scheduler;

import javax.inject.Provider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class DefaultJobRegistry implements JobRegistry {

    private final Provider<Scheduler> scheduler;
    private final Collection<MappedJobListener> listeners;
    private final Map<String, Job> standaloneJobs;
    private final Map<String, JobDefinition> allJobDefinitions;
    private final Set<String> allJobsAndGroupNames;

    // Lazily populated map of decorated runnable jobs (either standalone or groups)
    private final ConcurrentMap<String, Job> decoratedJobAndGroups;

    public DefaultJobRegistry(
            Collection<Job> standaloneJobs,
            Map<String, JobDefinition> jobDefinitions,
            Provider<Scheduler> scheduler,
            Collection<MappedJobListener> listeners) {

        this.allJobsAndGroupNames = allJobsAndGroupNames(standaloneJobs, jobDefinitions);
        this.allJobDefinitions = allJobDefinitions(jobDefinitions, standaloneJobs);
        this.standaloneJobs = jobsByName(standaloneJobs);
        this.decoratedJobAndGroups = new ConcurrentHashMap<>((int) (jobDefinitions.size() / 0.75d) + 1);
        this.scheduler = scheduler;
        this.listeners = listeners;
    }

    private Set<String> allJobsAndGroupNames(Collection<Job> jobs, Map<String, JobDefinition> jobDefinitions) {
        Set<String> jobNames = jobs.stream().map(job -> job.getMetadata().getName()).collect(Collectors.toSet());
        jobNames.addAll(jobDefinitions.keySet());
        return Collections.unmodifiableSet(jobNames);
    }

    private Map<String, JobDefinition> allJobDefinitions(
            Map<String, JobDefinition> configured,
            Collection<Job> standaloneJobs) {

        // combine explicit job definitions from config with default definitions for the existing jobs
        Map<String, JobDefinition> combined = new HashMap<>(configured);

        // create definition for each job, that is not present in config
        standaloneJobs.stream().map(j -> j.getMetadata().getName())
                .filter(n -> !combined.containsKey(n))
                .forEach(n -> combined.put(n, new SingleJobDefinition()));

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

        DIGraph<JobExecution> graph = new JobGraphBuilder(allJobDefinitions, standaloneJobs).createGraph(jobName);
        List<Job> standaloneJobsInGraph = standaloneJobsInGraph(graph);

        if (standaloneJobsInGraph.size() != 1) {
            return new JobGroup(jobName, standaloneJobsInGraph, graph, scheduler.get(), listeners);
        }

        Job job = standaloneJobsInGraph.get(0);
        Job withName = decorateWithName(jobName, job);

        JobExecution exec = graph.topSort().get(0).iterator().next();
        return new ListenerAwareJob(withName, exec.getParams(), listeners);
    }

    @Override
    public boolean allowsSimultaneousExecutions(String jobName) {
        checkJobExists(jobName);

        Job job = standaloneJobs.get(jobName);
        // simultaneous executions are allowed for job groups (in this case job is null)
        // and real jobs, that haven't been annotated with @SerialJob
        return job == null || !job.getClass().isAnnotationPresent(SerialJob.class);
    }

    private Map<String, Job> jobsByName(Collection<Job> jobs) {
        return jobs.stream().collect(HashMap::new, (m, j) -> m.put(j.getMetadata().getName(), j), HashMap::putAll);
    }

    /**
     * Optionally decorates a job with a different name. Decoration may be needed if we need to execute a job group
     * with a single job.
     */
    private Job decorateWithName(String name, Job job) {
        JobMetadata metadata = job.getMetadata();
        if (metadata.getName().equals(name)) {
            return job;
        }

        JobMetadata.Builder builder = JobMetadata.builder(name);
        metadata.getParameters().forEach(builder::param);
        return new JobMetadataDecorator(builder.build(), job);
    }

    private List<Job> standaloneJobsInGraph(DIGraph<JobExecution> graph) {
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
