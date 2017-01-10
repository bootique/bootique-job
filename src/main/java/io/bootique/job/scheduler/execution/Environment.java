package io.bootique.job.scheduler.execution;

import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.SingleJobDefinition;

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
            } else if (definition instanceof SingleJobDefinition) {
                JobDefinition delegateDefinition = defaultEnvironment.get().getDefinition(jobName);
                if (delegateDefinition instanceof SingleJobDefinition) {
                    definition = mergeDefinitions((SingleJobDefinition) definition, (SingleJobDefinition) delegateDefinition);
                }
            }
        }
        return Objects.requireNonNull(definition, "Unknown job: " + jobName);
    }

    private SingleJobDefinition mergeDefinitions(SingleJobDefinition overriding, SingleJobDefinition overriden) {
        return new SingleJobDefinition(mergeParams(overriding, overriden), mergeDependencies(overriding, overriden));
    }

    private Map<String, Object> mergeParams(SingleJobDefinition overriding, SingleJobDefinition overriden) {
        Map<String, Object> params = new HashMap<>(overriden.getParams());
        params.putAll(overriding.getParams());
        return params;
    }

    private Optional<List<String>> mergeDependencies(SingleJobDefinition overriding, SingleJobDefinition overriden) {
        return overriding.getDependsOn().isPresent() ? overriding.getDependsOn() : overriden.getDependsOn();
    }
}
