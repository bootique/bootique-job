package io.bootique.job.scheduler.execution;

import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.SingleJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

class Environment {

    private Map<String, ? extends JobDefinition> definitions;
    private Optional<Environment> defaultEnvironment;

    public Environment(Map<String, ? extends JobDefinition> definitions) {
        this(definitions, null);
    }

    public Environment(Map<String, ? extends JobDefinition> definitions,
                       Environment defaultEnvironment) {
        this.definitions = definitions;
        this.defaultEnvironment = Optional.ofNullable(defaultEnvironment);
    }

    public JobDefinition getDefinition(String jobName) {
        JobDefinition definition = definitions.get(jobName);
        if (defaultEnvironment.isPresent()) {
            if (definition == null) {
                definition = defaultEnvironment.get().getDefinition(jobName);
            } else if (definition instanceof SingleJob) {
                JobDefinition delegateDefinition = defaultEnvironment.get().getDefinition(jobName);
                if (delegateDefinition instanceof SingleJob) {
                    definition = mergeDefinitions((SingleJob) definition, (SingleJob) delegateDefinition);
                }
            }
        }
        return Objects.requireNonNull(definition, "Unknown job: " + jobName);
    }

    private SingleJob mergeDefinitions(SingleJob overriding, SingleJob overriden) {
        return new SingleJob(mergeParams(overriding, overriden), mergeDependencies(overriding, overriden));
    }

    private Map<String, Object> mergeParams(SingleJob overriding, SingleJob overriden) {
        Map<String, Object> params = new HashMap<>(overriden.getParams());
        params.putAll(overriding.getParams());
        return params;
    }

    private Optional<List<String>> mergeDependencies(SingleJob overriding, SingleJob overriden) {
        return overriding.getDependsOn().isPresent() ? overriding.getDependsOn() : overriden.getDependsOn();
    }
}
