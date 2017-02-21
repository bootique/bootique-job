package io.bootique.job;

import io.bootique.job.runnable.JobResult;

import java.util.Map;
import java.util.function.Consumer;

/**
 * A listener that will be notified of every started job. When a job is started the listener may optionally decide to
 * get notified when this particular job is finished by registering a callback function with provided event source.
 *
 * @since 0.14
 */
public interface JobListener {

    /**
     * A method invoked when a job is started. The listener may optionally decide to get notified when the job is
     * finished by registering a callback function with provided "finishEventSource".
     *
     * @param jobName           the name of a job that generated start event.
     * @param parameters        parameters passed to the job.
     * @param finishEventSource an object that will notify registered consumers when the job that generated this start
     *                          event is finished.
     */
    void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource);
}
