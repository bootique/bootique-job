package io.bootique.job.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @since 0.13
 */
@BQConfig("Standalone job with optional dependencies.")
@JsonTypeName("single")
public class SingleJobDefinition implements JobDefinition {

    private Map<String, Object> params;
    private Optional<List<String>> dependsOn;

    public SingleJobDefinition() {
        this.params = Collections.emptyMap();
        this.dependsOn = Optional.empty();
    }

    public SingleJobDefinition(Map<String, Object> params, Optional<List<String>> dependsOn) {
        this.params = params;
        this.dependsOn = dependsOn;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    @BQConfigProperty
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Optional<List<String>> getDependsOn() {
        return dependsOn;
    }

    @BQConfigProperty("List of dependencies, that should be run prior to the current job." +
            " May include names of both standalone jobs and job groups." +
            " Note that the order of execution of dependencies may be different from the order, in which they appear in this list." +
            " If you'd like the dependencies to be executed in a particular order, consider creating an explicit job group.")
    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = Optional.of(dependsOn);
    }
}
