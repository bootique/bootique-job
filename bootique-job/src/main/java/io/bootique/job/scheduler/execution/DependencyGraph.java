package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobParameterMetadata;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.JobGroupDefinition;
import io.bootique.job.config.SingleJobDefinition;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DependencyGraph {

    private final Map<String, JobExecution> knownExecutions = new LinkedHashMap<>();
    private final DIGraph<JobExecution> graph;
    private final Map<String, Job> jobs;

    DependencyGraph(String rootJobName, Map<String, JobDefinition> definitionMap, Map<String, Job> jobs) {
        DIGraph<JobExecution> graph = new DIGraph<>();
        this.jobs = jobs;
        Environment jobDefinitions = new Environment(definitionMap);
        populateWithDependencies(rootJobName, null, graph, jobDefinitions, new HashMap<>());
        this.graph = graph;
    }

    private void populateWithDependencies(String jobName,
                                          JobExecution childExecution,
                                          DIGraph<JobExecution> graph,
                                          Environment jobDefinitions,
                                          Map<String, JobExecution> childExecutions) {

        JobDefinition jobDefinition = jobDefinitions.getDefinition(jobName);
        if (jobDefinition instanceof SingleJobDefinition) {
            SingleJobDefinition singleJob = (SingleJobDefinition) jobDefinition;
            JobExecution execution = getOrCreateExecution(jobName, singleJob);
            graph.add(execution);
            if (childExecution != null) {
                graph.add(execution, childExecution);
            }
            populateWithSingleJobDependencies(execution, graph, jobDefinitions, childExecutions);

        } else if (jobDefinition instanceof JobGroupDefinition) {
            JobGroupDefinition group = (JobGroupDefinition) jobDefinition;
            group.getJobs().forEach((name, definition) -> {
                Environment groupDefinitions = new Environment(group.getJobs(), jobDefinitions);
                populateWithDependencies(name, childExecution, graph, groupDefinitions, childExecutions);
            });

        } else {
            throw createUnexpectedJobDefinitionError(jobDefinition);
        }
    }

    private void populateWithSingleJobDependencies(JobExecution execution,
                                                   DIGraph<JobExecution> graph,
                                                   Environment jobDefinitions,
                                                   Map<String, JobExecution> childExecutions) {
        String jobName = execution.getJobName();
        childExecutions.put(jobName, execution);
        ((SingleJobDefinition) jobDefinitions.getDefinition(jobName)).getDependsOn().ifPresent(parents ->
            parents.forEach(parentName -> {
                if (childExecutions.containsKey(parentName)) {
                    throw new IllegalStateException(String.format("Cycle: [...] -> %s -> %s", jobName, parentName));
                }
                populateWithDependencies(parentName, execution, graph, jobDefinitions, childExecutions);
            }));
        childExecutions.remove(jobName);
    }

    private JobExecution getOrCreateExecution(String jobName, SingleJobDefinition definition) {
        JobExecution execution = knownExecutions.get(jobName);
        if (execution == null) {
            execution = new JobExecution(jobName, convertParams(jobs.get(jobName).getMetadata(), definition.getParams()));
            knownExecutions.put(jobName, execution);
        }
        return execution;
    }

    private Map<String, Object> convertParams(JobMetadata jobMD, Map<String, String> params) {
        // clone params map in order to preserve parameters that were not specified in metadata
		Map<String, Object> convertedParams = new HashMap<>(params);
		for (JobParameterMetadata<?> param : jobMD.getParameters()) {
			String valueString = params.get(param.getName());
			Object value = param.fromString(valueString);
			convertedParams.put(param.getName(), value);
		}
		return convertedParams;
	}

    private RuntimeException createUnexpectedJobDefinitionError(JobDefinition definition) {
        return new IllegalArgumentException("Unexpected job definition type: " + definition.getClass().getName());
    }

    public List<Set<JobExecution>> topSort() {
        return graph.topSort();
    }

    public Set<String> getJobNames() {
        return knownExecutions.keySet();
    }
}
