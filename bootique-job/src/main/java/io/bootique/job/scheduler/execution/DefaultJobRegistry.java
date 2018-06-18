/**
 *  Licensed to ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.job.scheduler.execution;

import com.google.inject.Provider;
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobRegistry;
import io.bootique.job.MappedJobListener;
import io.bootique.job.SerialJob;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.SingleJobDefinition;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @since 0.13
 */
public class DefaultJobRegistry implements JobRegistry {

    /**
     * Combined list of single job names and group names,
     * i.e. everything that can be "run"
     */
    private Set<String> availableJobs;

    /**
     * "Real" job implementations (no groups here)
     */
    private Map<String, Job> jobs;

    /**
     * All single job and group definitions, that were specified in configuration
     */
    private Map<String, JobDefinition> jobDefinitions;

    /**
     * Combined collection of single jobs and job groups,
     * lazily populated upon request to retrieve a particular job
     */
    private ConcurrentMap<String, Job> executions;

    private Provider<Scheduler> schedulerProvider;
    private Collection<MappedJobListener> listeners;

    public DefaultJobRegistry(Collection<Job> jobs,
                              Map<String, JobDefinition> jobDefinitions,
                              Provider<Scheduler> schedulerProvider,
                              Collection<MappedJobListener> listeners) {
        this.availableJobs = Collections.unmodifiableSet(collectJobNames(jobs, jobDefinitions));
        this.jobs = mapJobs(jobs);
        this.jobDefinitions = collectJobDefinitions(jobDefinitions, jobs);
        this.executions = new ConcurrentHashMap<>((int) (jobDefinitions.size() / 0.75d) + 1);
        this.schedulerProvider = schedulerProvider;
        this.listeners = listeners;
    }

    private Map<String, JobDefinition> collectJobDefinitions(Map<String, JobDefinition> configured, Collection<Job> jobs) {
        Map<String, JobDefinition> combined = new HashMap<>(configured);
        // create definition for each job, that is not present in config
        jobs.stream().filter(job -> !combined.containsKey(job.getMetadata().getName())).forEach(job -> {
            combined.put(job.getMetadata().getName(), new SingleJobDefinition());
        });
        return combined;
    }

    private Set<String> collectJobNames(Collection<Job> jobs, Map<String, JobDefinition> jobDefinitions) {
        Set<String> jobNames = jobs.stream().map(job -> job.getMetadata().getName()).collect(Collectors.toSet());
        jobNames.addAll(jobDefinitions.keySet());
        return jobNames;
    }

    @Override
    public Set<String> getAvailableJobs() {
        return availableJobs;
    }

    @Override
    public Job getJob(String jobName) {
        Job execution = executions.get(jobName);
        if (execution == null) {
            DependencyGraph graph = new DependencyGraph(jobName, jobDefinitions, jobs);
            Collection<Job> executionJobs = collectJobs(graph);
            if (executionJobs.size() == 1) {
                // do not create a full-fledged execution for standalone jobs
                Job job = executionJobs.iterator().next();
                JobMetadata jobMetadata = cloneMetadata(jobName, job.getMetadata());
                Job delegate = new Job() {
                    @Override
                    public JobMetadata getMetadata() {
                        return jobMetadata;
                    }

                    @Override
                    public JobResult run(Map<String, Object> parameters) {
                        return job.run(parameters);
                    }
                };
                execution = new SingleJob(delegate, graph.topSort().get(0).iterator().next(), listeners);
            } else {
                execution = new JobGroup(jobName, executionJobs, graph, schedulerProvider.get(), listeners);
            }

            Job existing = executions.putIfAbsent(jobName, execution);
            if (existing != null) {
                execution = existing;
            }
        }
        return execution;
    }

    @Deprecated
    @Override
    public boolean allowsSimlutaneousExecutions(String jobName) {
        return allowsSimultaneousExecutions(jobName);
    }

    /**
     * @since 0.26
     */
    @Override
    public boolean allowsSimultaneousExecutions(String jobName) {
        if (!availableJobs.contains(jobName)) {
            throw new IllegalArgumentException("Unknown job: " + jobName);
        }
        Job job = jobs.get(jobName);
        // simultaneous executions are allowed for job groups (in this case job is null)
        // and real jobs, that haven't been annotated with @SerialJob
        return (job == null) || !job.getClass().isAnnotationPresent(SerialJob.class);
    }

    private Map<String, Job> mapJobs(Collection<Job> jobs) {
        return jobs.stream().collect(HashMap::new, (m, j) -> m.put(j.getMetadata().getName(), j), HashMap::putAll);
    }

    private JobMetadata cloneMetadata(String newName, JobMetadata metadata) {
        JobMetadata.Builder builder = JobMetadata.builder(newName);
        metadata.getParameters().forEach(builder::param);
        return builder.build();
    }

    private Collection<Job> collectJobs(DependencyGraph graph) {
        return jobs.entrySet().stream()
                .filter(e -> graph.getJobNames().contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}
