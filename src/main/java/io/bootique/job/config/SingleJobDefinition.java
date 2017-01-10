package io.bootique.job.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @since 0.13
 */
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

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Optional<List<String>> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = Optional.of(dependsOn);
    }
}
