package io.bootique.job.scheduler;

import io.bootique.job.runnable.JobFuture;

import java.util.Map;

public interface Scheduler {

    /**
     * Executes a given job once.
     *
     * @param jobName the name of the job to execute.
     * @return a Future to track job progress.
     */
    JobFuture runOnce(String jobName);

    /**
     * Executes a given job once.
     *
     * @param jobName    the name of the job to execute.
     * @param parameters a Map of parameters that will be merged with the DI-provided parameters for this execution.
     * @return a Future to track job progress.
     * @since 0.13
     */
    JobFuture runOnce(String jobName, Map<String, Object> parameters);

    int start();
}
