package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;

import java.util.HashMap;
import java.util.Map;

class SingleJob implements Job {

    private Job delegate;
    private JobExecution execution;

    SingleJob(Job delegate, JobExecution execution) {
        this.delegate = delegate;
        this.execution = execution;
    }

    @Override
    public JobMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public JobResult run(Map<String, Object> parameters) {
        Map<String, Object> mergedParams = mergeParams(parameters, execution.getParams());
        return delegate.run(mergedParams);
    }

    private Map<String, Object> mergeParams(Map<String, Object> overridingParams, Map<String, Object> defaultParams) {
        Map<String, Object> merged = new HashMap<>(defaultParams);
        merged.putAll(overridingParams);
        return merged;
    }
}
