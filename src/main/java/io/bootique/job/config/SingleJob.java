package io.bootique.job.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SingleJob implements JobDefinition {

    private Map<String, Object> params;
    private Optional<List<String>> dependsOn;

    public SingleJob() {
        this.params = Collections.emptyMap();
        this.dependsOn = Optional.empty();
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
