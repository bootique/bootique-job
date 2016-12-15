package io.bootique.job.scheduler.execution;

import io.bootique.job.config.SingleJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class OverridingJobDefinition extends SingleJob {

    private Map<String, Object> params;
    private Optional<List<String>> dependsOn;

    OverridingJobDefinition(SingleJob overriding, SingleJob overriden) {
        this.params = mergeParams(overriding, overriden);
        this.dependsOn = getDependencies(overriding, overriden);
    }

    private Map<String, Object> mergeParams(SingleJob overriding, SingleJob overriden) {
        Map<String, Object> params = new HashMap<>(overriden.getParams());
        params.putAll(overriding.getParams());
        return params;
    }

    private Optional<List<String>> getDependencies(SingleJob overriding, SingleJob overriden) {
        return overriding.getDependsOn().isPresent() ? overriding.getDependsOn() : overriden.getDependsOn();
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public Optional<List<String>> getDependsOn() {
        return dependsOn;
    }

    @Override
    public void setParams(Map<String, Object> params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDependsOn(List<String> dependsOn) {
        throw new UnsupportedOperationException();
    }
}
