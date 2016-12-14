package io.bootique.job.scheduler.execution;

import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.SingleJob;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

class NestedJobDefinitions {

    private Map<String, ? extends JobDefinition> definitions;
    private Optional<NestedJobDefinitions> defaultDefinitions;

    public NestedJobDefinitions(Map<String, ? extends JobDefinition> definitions) {
        this(definitions, null);
    }

    public NestedJobDefinitions(Map<String, ? extends JobDefinition> definitions,
                                NestedJobDefinitions defaultDefinitions) {
        this.definitions = definitions;
        this.defaultDefinitions = Optional.ofNullable(defaultDefinitions);
    }

    public JobDefinition getDefinition(String jobName) {
        JobDefinition definition = definitions.get(jobName);
        if (defaultDefinitions.isPresent()) {
            if (definition == null) {
                definition = defaultDefinitions.get().getDefinition(jobName);
            } else if (definition instanceof SingleJob) {
                JobDefinition delegateDefinition = defaultDefinitions.get().getDefinition(jobName);
                if (delegateDefinition instanceof SingleJob) {
                    definition = new OverridingJobDefinition((SingleJob) definition, (SingleJob) delegateDefinition);
                }
            }
        }
        return Objects.requireNonNull(definition, "Unknown job: " + jobName);
    }
}
