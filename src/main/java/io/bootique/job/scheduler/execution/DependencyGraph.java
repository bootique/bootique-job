package io.bootique.job.scheduler.execution;

import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.JobGroup;
import io.bootique.job.config.SingleJob;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DependencyGraph {

    private final Map<String, SingleJobExecution> knownExecutions = new LinkedHashMap<>();
    private final DIGraph<SingleJobExecution> graph;

    DependencyGraph(String rootJobName, Map<String, JobDefinition> definitionMap) {
        DIGraph<SingleJobExecution> graph = new DIGraph<>();
        Environment jobDefinitions = new Environment(definitionMap);
        populateWithDependencies(rootJobName, null, graph, jobDefinitions, new HashMap<>());
        this.graph = graph;
    }

    private void populateWithDependencies(String jobName,
                                          SingleJobExecution childExecution,
                                          DIGraph<SingleJobExecution> graph,
                                          Environment jobDefinitions,
                                          Map<String, SingleJobExecution> childExecutions) {

        JobDefinition jobDefinition = jobDefinitions.getDefinition(jobName);
        if (jobDefinition instanceof SingleJob) {
            SingleJob singleJob = (SingleJob) jobDefinition;
            SingleJobExecution execution = getOrCreateExecution(jobName, singleJob);
            graph.add(execution);
            if (childExecution != null) {
                graph.add(execution, childExecution);
            }
            populateWithSingleJobDependencies(execution, graph, jobDefinitions, childExecutions);

        } else if (jobDefinition instanceof JobGroup) {
            JobGroup group = (JobGroup) jobDefinition;
            group.getJobs().forEach((name, definition) -> {
                Environment groupDefinitions = new Environment(group.getJobs(), jobDefinitions);
                populateWithDependencies(name, childExecution, graph, groupDefinitions, childExecutions);
            });

        } else {
            throw createUnexpectedJobDefinitionError(jobDefinition);
        }
    }

    private void populateWithSingleJobDependencies(SingleJobExecution execution,
                                                   DIGraph<SingleJobExecution> graph,
                                                   Environment jobDefinitions,
                                                   Map<String, SingleJobExecution> childExecutions) {
        String jobName = execution.getJobName();
        childExecutions.put(jobName, execution);
        ((SingleJob) jobDefinitions.getDefinition(jobName)).getDependsOn().ifPresent(parents ->
            parents.forEach(parentName -> {
                if (childExecutions.containsKey(parentName)) {
                    throw new IllegalStateException(String.format("Cycle: [...] -> %s -> %s", jobName, parentName));
                }
                populateWithDependencies(parentName, execution, graph, jobDefinitions, childExecutions);
            }));
        childExecutions.remove(jobName);
    }

    private SingleJobExecution getOrCreateExecution(String jobName, SingleJob definition) {
        SingleJobExecution execution = knownExecutions.get(jobName);
        if (execution == null) {
            execution = new SingleJobExecution(jobName, definition.getParams());
            knownExecutions.put(jobName, execution);
        }
        return execution;
    }

    private RuntimeException createUnexpectedJobDefinitionError(JobDefinition definition) {
        return new IllegalArgumentException("Unexpected job definition type: " + definition.getClass().getName());
    }

    public List<Set<SingleJobExecution>> topSort() {
        return graph.topSort();
    }

    public Set<String> getJobNames() {
        return knownExecutions.keySet();
    }
}
