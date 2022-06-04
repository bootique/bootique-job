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
import io.bootique.job.descriptor.JobDescriptor;
import io.bootique.job.descriptor.SingleJobDescriptor;
import io.bootique.job.scheduler.Scheduler;
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
    protected final Collection<MappedJobListener> listeners;
    protected final Map<String, Job> standaloneJobs;
    protected final Map<String, JobDescriptor> allJobDescriptors;
    protected final Set<String> allJobsAndGroupNames;

    // Lazily populated map of decorated runnable jobs (either standalone or groups)
    private final ConcurrentMap<String, Job> decoratedJobAndGroups;

    public DefaultJobRegistry(
            Collection<Job> standaloneJobs,
            Map<String, JobDescriptor> jobDescriptors,
            Provider<Scheduler> scheduler,
            Collection<MappedJobListener> listeners) {

        this.standaloneJobs = jobsByName(standaloneJobs);
        this.allJobsAndGroupNames = allJobsAndGroupNames(this.standaloneJobs, jobDescriptors);
        this.allJobDescriptors = allDescriptors(this.standaloneJobs.keySet(), jobDescriptors);
        this.decoratedJobAndGroups = new ConcurrentHashMap<>((int) (jobDescriptors.size() / 0.75d) + 1);
        this.scheduler = scheduler;
        this.listeners = listeners;
    }

    private Set<String> allJobsAndGroupNames(
            Map<String, Job> standaloneJobs,
            Map<String, JobDescriptor> jobDefinitions) {

        // TODO: how do we check for conflicts between standalone job names and group names?
        Set<String> jobNames = new HashSet<>(standaloneJobs.keySet());
        jobNames.addAll(jobDefinitions.keySet());
        return jobNames;
    }

    private Map<String, JobDescriptor> allDescriptors(
            Set<String> standaloneJobsNames,
            Map<String, JobDescriptor> configured) {

        // combine explicit job descriptors from config with default descriptors for the existing jobs
        Map<String, JobDescriptor> combined = new HashMap<>(configured);

        // create descriptor for each job, that is not present in config
        standaloneJobsNames.stream()
                .filter(n -> !combined.containsKey(n))
                .forEach(n -> combined.put(n, new SingleJobDescriptor()));

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

    @Override
    public boolean allowsSimultaneousExecutions(String jobName) {
        checkJobExists(jobName);

        Job job = standaloneJobs.get(jobName);
        // simultaneous executions are allowed for job groups (in this case job is null)
        // and real jobs, that haven't been annotated with @SerialJob
        return job == null || !job.getClass().isAnnotationPresent(SerialJob.class);
    }

    /**
     * @since 3.0
     */
    protected Job createJob(String jobName) {

        checkJobExists(jobName);

        DIGraph<JobExecution> graph = new JobGraphBuilder(allJobDescriptors, standaloneJobs).createGraph(jobName);
        List<Job> standaloneJobsInGraph = standaloneJobsInGraph(graph);

        switch (standaloneJobsInGraph.size()) {
            case 1:
                JobExecution exec = graph.topSort().get(0).iterator().next();
                Job job = standaloneJobsInGraph.get(0);
                return decorateJob(job, jobName, exec.getParams());
            case 0:
                // fall through to the JobGroup
                LOGGER.warn("Job group '{}' is empty. It is valid, but will do nothing", jobName);
            default:
                Job group = createJobGroup(jobName, graph);
                return decorateJob(group, jobName, Collections.emptyMap());
        }
    }

    protected JobGroup createJobGroup(String jobName, DIGraph<JobExecution> graph) {
        JobMetadata groupMetadata = groupMetadata(jobName, standaloneJobs.values());
        List<Set<Job>> executionPlan = executionPlan(graph.reverseTopSort(), standaloneJobs);
        return createJobGroup(groupMetadata, executionPlan);
    }

    protected JobGroup createJobGroup(JobMetadata groupMetadata, List<Set<Job>> executionPlan) {
        return new JobGroup(groupMetadata, executionPlan, scheduler.get());
    }

    private JobMetadata groupMetadata(String jobName, Collection<Job> jobs) {
        JobMetadata.Builder builder = JobMetadata.builder(jobName);
        for (Job job : jobs) {
            job.getMetadata().getParameters().forEach(builder::param);
        }
        return builder.build();
    }

    private List<Set<Job>> executionPlan(List<Set<JobExecution>> executions, Map<String, Job> standaloneJobs) {

        List<Set<Job>> result = new ArrayList<>(executions.size());

        for (Set<JobExecution> s : executions) {
            Set<Job> executionGroup = new HashSet<>();
            for (JobExecution e : s) {

                Job undecorated = standaloneJobs.get(e.getJobName());
                Job decorated = decorateGroupMemberJob(undecorated, e.getParams());

                executionGroup.add(decorated);
            }
            result.add(executionGroup);
        }

        return result;
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

    protected Job decorateJob(Job undecorated, String altName, Map<String, Object> prebindParams) {
        Job withName = decorateWithName(undecorated, altName);
        Job withListeners = decorateWithListeners(withName, listeners);

        // parameter decorator must go AFTER the listeners decorator to enable listeners to receive
        // curried parameter values
        Job withParamBindings = decorateWithParamBindings(withListeners, prebindParams);

        // finally, catch exceptions and log the outcome. Note that while listeners decorator catches Job exceptions,
        // this handler will only catch listener exceptions
        Job withExceptionsCaught = decorateWithExceptionsHandler(withParamBindings);
        return decorateWithLogger(withExceptionsCaught);
    }

    protected Job decorateGroupMemberJob(Job undecorated, Map<String, Object> prebindParams) {
        return decorateWithParamBindings(undecorated, prebindParams);
    }

    /**
     * Optionally decorates a job with a different name. Decoration may be needed if we need to execute a job group
     * with a single job.
     */
    protected Job decorateWithName(Job job, String name) {
        JobMetadata metadata = job.getMetadata();
        if (metadata.getName().equals(name)) {
            return job;
        }

        JobMetadata.Builder builder = JobMetadata.builder(name);
        metadata.getParameters().forEach(builder::param);
        return new JobMetadataDecorator(job, builder.build());
    }

    protected Job decorateWithListeners(Job job, Collection<MappedJobListener> listeners) {
        return listeners.isEmpty() ? job : new JobListenerDecorator(job, listeners);
    }

    protected Job decorateWithParamBindings(Job job, Map<String, Object> params) {
        return params.isEmpty() ? job : new JobParamDefaultsDecorator(job, params);
    }

    protected Job decorateWithExceptionsHandler(Job job) {
        return new JobExceptionsHandlerDecorator(job);
    }

    protected Job decorateWithLogger(Job job) {
        return new JobLogger(job);
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
