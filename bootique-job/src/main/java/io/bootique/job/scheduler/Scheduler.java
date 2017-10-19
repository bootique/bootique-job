package io.bootique.job.scheduler;

import io.bootique.BootiqueException;
import io.bootique.job.Job;
import io.bootique.job.runnable.JobFuture;

import java.util.Collection;
import java.util.List;
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

    /**
     * Executes a given job once.
     *
     * @param job Job to execute
     * @return a Future to track job progress.
     * @since 0.13
     */
    JobFuture runOnce(Job job);

    /**
     * Executes a given job once.
     *
     * @param job Job to execute
     * @param parameters a Map of parameters that will be merged with the DI-provided parameters for this execution.
     * @return a Future to track job progress.
     * @since 0.13
     */
    JobFuture runOnce(Job job, Map<String, Object> parameters);

    /**
     * Schedule execution of jobs based on configured triggers.
     * Throws an exception, if the scheduler has already been started
     *
     * @return Number of scheduled jobs, possibly zero
     */
    int start();

    /**
     * Schedule execution of jobs based on configured triggers.
     * Throws an exception, if the scheduler has already been started
     *
     * @param jobNames Jobs to schedule
     * @return Number of scheduled jobs, possibly zero
     * @throws BootiqueException if {@code jobNames} is null or empty or some of the jobs are unknown
     * @since 0.25
     */
    int start(List<String> jobNames);

    /**
     * @return true, if the scheduler has been started
     */
    boolean isStarted();

    /**
     * Returns a collection of triggers, configured for this scheduler.
     *
     * @return Collection of triggers, configured for this scheduler.
     * @since 0.13
     * @deprecated since 0.24 in favor of {@link #getScheduledJobs()}
     */
    @Deprecated
    Collection<TriggerDescriptor> getTriggers();

    /**
     * @return Collection of scheduled job executions for all known jobs
     * @since 0.24
     */
    Collection<ScheduledJobFuture> getScheduledJobs();

    /**
     * @param jobName Job name
     * @return Scheduled job executions for a given job, or an empty collection, if the job is unknown,
     *         triggers are not configured for this job or the scheduler has not been started yet
     * @since 0.24
     */
    Collection<ScheduledJobFuture> getScheduledJobs(String jobName);
}
