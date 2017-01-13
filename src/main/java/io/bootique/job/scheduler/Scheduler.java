package io.bootique.job.scheduler;

import io.bootique.job.Job;
import io.bootique.job.runnable.JobFuture;

import java.util.Collection;
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

    int start();

    /**
     * Returns a collection of triggers, configured for this scheduler.
     *
     * @return Collection of triggers, configured for this scheduler.
     * @since 0.13
     */
    Collection<TriggerDescriptor> getTriggers();

    /**
     * Returns jobs that are currently submitted for execution.
     *
     * @return Collection of jobs that were submitted for execution but hadn't yet completed.
     * @since 0.13
     */
    Collection<JobFuture> getSubmittedJobs();
}
