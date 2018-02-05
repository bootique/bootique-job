package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.MappedJobListener;
import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class SingleJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleJob.class);

    private Job delegate;
    private JobExecution execution;
    private Collection<MappedJobListener> listeners;

    SingleJob(Job delegate, JobExecution execution, Collection<MappedJobListener> listeners) {
        this.delegate = delegate;
        this.execution = execution;
        this.listeners = listeners;
    }

    @Override
    public JobMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public JobResult run(Map<String, Object> parameters) {
        Map<String, Object> mergedParams = mergeParams(parameters, execution.getParams());

        try {
            return Callback.runAndNotify(delegate, mergedParams, listeners);
        } catch (Exception e) {
            LOGGER.error(delegate.getMetadata().getName() + "failed to run.", e);
            return JobResult.failure(delegate.getMetadata());
        }
    }

    private Map<String, Object> mergeParams(Map<String, Object> overridingParams, Map<String, Object> defaultParams) {
        Map<String, Object> merged = new HashMap<>(defaultParams);
        merged.putAll(overridingParams);
        return merged;
    }
}
