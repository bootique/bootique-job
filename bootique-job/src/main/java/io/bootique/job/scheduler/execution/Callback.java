package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.MappedJobListener;
import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

class Callback implements Consumer<Consumer<JobResult>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Callback.class);

    static JobResult runAndNotify(Job job, Map<String, Object> parameters, Set<MappedJobListener> listeners) {
        String jobName = job.getMetadata().getName();
        Optional<Callback> callbackOptional = listeners.isEmpty() ? Optional.empty() : Optional.of(new Callback(jobName));

        callbackOptional.ifPresent(callback -> {
            listeners.forEach(listener -> {
                try {
                    listener.getListener().onJobStarted(jobName, parameters, callback);
                } catch (Exception e) {
                    LOGGER.error("Error invoking job listener for job: " + jobName, e);
                }
            });
        });

        JobResult result;
        try {
            result = job.run(parameters);
        } catch (Exception e) {
            result = JobResult.failure(job.getMetadata(), e);
            if (callbackOptional.isPresent()) {
                callbackOptional.get().invoke(result);
            }
        }
        if (result == null) {
            result = JobResult.unknown(job.getMetadata());
        }
        if (callbackOptional.isPresent()) {
            callbackOptional.get().invoke(result);
        }
        return result;
    }

    private String jobName;
    private LinkedList<Consumer<JobResult>> callbacks;

    public Callback(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public void accept(Consumer<JobResult> callback) {
        if (callbacks == null) {
            callbacks = new LinkedList<>();
        }
        callbacks.add(callback);
    }

    public void invoke(JobResult result) {
        Iterator<Consumer<JobResult>> itr = callbacks.descendingIterator();
        while (itr.hasNext()) {
            try {
                itr.next().accept(result);
            } catch (Exception e) {
                LOGGER.error("Error invoking completion callback for job: " + jobName, e);
            }
        }
    }
}