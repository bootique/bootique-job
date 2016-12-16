package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.scheduler.DefaultScheduler;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ExecutionFactory {

    private Collection<Job> jobs;
    private Map<String, JobDefinition> jobDefinitions;
    private ConcurrentMap<String, Execution> executions;
    private DefaultScheduler scheduler;

    public ExecutionFactory(Collection<Job> jobs, Map<String, JobDefinition> jobDefinitions, DefaultScheduler scheduler) {
        this.jobs = jobs;
        this.jobDefinitions = jobDefinitions;
        this.executions = new ConcurrentHashMap<>((int)(jobDefinitions.size() / 0.75d) + 1);
        this.scheduler = scheduler;
    }

    public Execution getExecution(String jobName) {
        Execution execution = executions.get(jobName);
        if (execution == null) {
            DependencyGraph graph = new DependencyGraph(jobName, jobDefinitions);
            execution = new Execution(jobName, collectJobs(graph), graph, scheduler);
            Execution existing = executions.putIfAbsent(jobName, execution);
            if (existing != null) {
                execution = existing;
            }
        }
        return execution;
    }

    private Collection<Job> collectJobs(DependencyGraph graph) {
        return jobs.stream()
                .filter(job -> graph.getJobNames().contains(job.getMetadata().getName()))
                .collect(Collectors.toList());
    }

}
