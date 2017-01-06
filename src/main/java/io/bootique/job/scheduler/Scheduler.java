package io.bootique.job.scheduler;

import io.bootique.job.Job;
import io.bootique.job.runnable.JobFuture;
import org.springframework.scheduling.Trigger;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

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

    /**
     * @param job
     * @param parameters
     * @param date
     * @return a Future to track job progress.
     * @since 0.13
     */
    JobFuture runOnce(Job job, Map<String, Object> parameters, Date date);

    /**
     * @param job
     * @param parameters
     * @param trigger
     * @return a Future to track job progress.
     * @since 0.13
     */
    ScheduledFuture<?> schedule(Job job, Map<String, Object> parameters, Trigger trigger);

    int start();
}
