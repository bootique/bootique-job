package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.config.JobDefinition;
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

public class DefaultExecutionFactory implements ExecutionFactory {

    private Set<String> availableJobs;

    private Collection<Job> jobs;
    private Map<String, JobDefinition> jobDefinitions;
    private ConcurrentMap<String, Execution> executions;
    private Scheduler scheduler;

    public DefaultExecutionFactory(Collection<Job> jobs, Map<String, JobDefinition> jobDefinitions, Scheduler scheduler) {
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
    public Execution getExecution(String jobName) {
        Execution execution = executions.get(jobName);
        if (execution == null) {
            DependencyGraph graph = new DependencyGraph(jobName, jobDefinitions);
            Collection<Job> executionJobs = collectJobs(graph);
            if (executionJobs.size() == 1) {
                // do not create a full-fledged execution for standalone jobs
                execution = new StandaloneExecution(executionJobs.iterator().next(), graph.topSort().get(0).iterator().next());
            } else {
                execution = new GroupExecution(jobName, executionJobs, graph, scheduler);
            }

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

    private static class StandaloneExecution implements Execution {

        private Job delegate;
        private JobExecution execution;

        StandaloneExecution(Job delegate, JobExecution execution) {
            this.delegate = delegate;
            this.execution = execution;
        }

        @Override
        public JobMetadata getMetadata() {
            return delegate.getMetadata();
        }

        @Override
        public JobResult run(Map<String, Object> parameters) {
            Map<String, Object> mergedParams = mergeParams(parameters, execution.getParams());
            return delegate.run(mergedParams);
        }

        private Map<String, Object> mergeParams(Map<String, Object> overridingParams, Map<String, Object> defaultParams) {
            Map<String, Object> merged = new HashMap<>(defaultParams);
            merged.putAll(overridingParams);
            return merged;
        }

        @Override
        public String getName() {
            return getMetadata().getName();
        }

        @Override
        public void traverseExecution(ExecutionVisitor visitor) {
            visitor.visitExecutionStep(Collections.singleton(execution));
        }
    }
}
