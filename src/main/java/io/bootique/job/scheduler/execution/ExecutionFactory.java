package io.bootique.job.scheduler.execution;

import io.bootique.job.config.JobDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExecutionFactory {

    private Map<String, JobDefinition> jobDefinitions;
    private ConcurrentMap<String, Execution> executions;

    public ExecutionFactory(Map<String, JobDefinition> jobDefinitions) {
        this.jobDefinitions = jobDefinitions;
        this.executions = new ConcurrentHashMap<>((int)(jobDefinitions.size() / 0.75d) + 1);
    }

    public Execution getExecution(String jobName) {
        Execution execution = executions.get(jobName);
        if (execution == null) {
            execution = new DependencyGraph(jobName, jobDefinitions);
            Execution existing = executions.putIfAbsent(jobName, execution);
            if (existing != null) {
                execution = existing;
            }
        }
        return execution;
    }
}
