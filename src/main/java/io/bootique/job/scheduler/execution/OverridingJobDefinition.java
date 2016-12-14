package io.bootique.job.scheduler.execution;

import io.bootique.job.config.SingleJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverridingJobDefinition extends SingleJob {

    private SingleJob delegate;
    private Map<String, Object> params;

    public OverridingJobDefinition(SingleJob overriding, SingleJob overriden) {
        this.delegate = overriding;
        this.params = collectParams(overriding, overriden);
    }

    private Map<String, Object> collectParams(SingleJob overriding, SingleJob overriden) {
        Map<String, Object> params = new HashMap<>(overriden.getParams());
        params.putAll(overriding.getParams());
        return params;
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public List<String> getDependsOn() {
        return delegate.getDependsOn();
    }

    @Override
    public void setParams(Map<String, Object> params) {
        delegate.setParams(params);
    }

    @Override
    public void setDependsOn(List<String> dependsOn) {
        delegate.setDependsOn(dependsOn);
    }
}
