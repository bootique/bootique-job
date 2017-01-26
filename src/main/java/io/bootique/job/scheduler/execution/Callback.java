package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobListener;
import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

class Callback implements Consumer<Consumer<JobResult>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Callback.class);

    static JobResult runAndNotify(Job job, Map<String, Object> parameters, Set<JobListener> listeners) {
        if (listeners.isEmpty()) {
            return job.run(parameters);
        }

        String jobName = job.getMetadata().getName();

        Callback callback = new Callback(jobName);
        listeners.forEach(listener -> {
            try {
                listener.onJobStarted(jobName, parameters, callback);
            } catch (Exception e) {
                LOGGER.error("Error invoking job listener for job: " + jobName, e);
            }
        });
        JobResult result;
        try {
            result = job.run(parameters);
        } catch (Exception e) {
            callback.invoke(JobResult.failure(job.getMetadata(), e));
            throw e;
        }
        if (result == null) {
            result = JobResult.unknown(job.getMetadata());
        }
        callback.invoke(result);
        return result;
    }

    private String jobName;
    private List<Consumer<JobResult>> callbacks;

    public Callback(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public void accept(Consumer<JobResult> callback) {
        if (callbacks == null) {
            callbacks = new ArrayList<>();
        }
        callbacks.add(callback);
    }

    public void invoke(JobResult result) {
        callbacks.forEach(cb -> {
            try {
                cb.accept(result);
            } catch (Exception e) {
                LOGGER.error("Error invoking completion callback for job: " + jobName, e);
            }
        });
    }
}
