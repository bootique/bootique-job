package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobRegistry;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @since 0.13
 */
public class DefaultJobRegistry implements JobRegistry {

    private Set<String> availableJobs;

    private Collection<Job> jobs;
    private Map<String, JobDefinition> jobDefinitions;
    private ConcurrentMap<String, Job> executions;
    private Scheduler scheduler;

    public DefaultJobRegistry(Collection<Job> jobs, Map<String, JobDefinition> jobDefinitions, Scheduler scheduler) {
        this.availableJobs = Collections.unmodifiableSet(collectJobNames(jobs, jobDefinitions));
        this.jobs = jobs;
        this.jobDefinitions = jobDefinitions;
        this.executions = new ConcurrentHashMap<>((int)(jobDefinitions.size() / 0.75d) + 1);
        this.scheduler = scheduler;
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
            DependencyGraph graph = new DependencyGraph(jobName, jobDefinitions);
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
                execution = new SingleJob(delegate, graph.topSort().get(0).iterator().next());
            } else {
                execution = new JobGroup(jobName, executionJobs, graph, scheduler);
            }

            Job existing = executions.putIfAbsent(jobName, execution);
            if (existing != null) {
                execution = existing;
            }
        }
        return execution;
    }

    private JobMetadata cloneMetadata(String newName, JobMetadata metadata) {
        JobMetadata.Builder builder = JobMetadata.builder(newName);
        metadata.getParameters().forEach(builder::param);
        return builder.build();
    }

    private Collection<Job> collectJobs(DependencyGraph graph) {
        return jobs.stream()
                .filter(job -> graph.getJobNames().contains(job.getMetadata().getName()))
                .collect(Collectors.toList());
    }
}
