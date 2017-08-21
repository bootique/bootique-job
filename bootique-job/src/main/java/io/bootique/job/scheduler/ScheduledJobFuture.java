package io.bootique.job.scheduler;

import io.bootique.job.runnable.JobFuture;

import java.util.Optional;

/**
 * @since 0.24
 */
public interface ScheduledJobFuture extends JobFuture {

    /**
     * Re-schedule this job based on the provided cron expression.
     * Has no effect, if the job has already been scheduled and hasn't finished yet.
     *
     * @param cron Cron expression
     * @return true, if the job has been re-scheduled.
     * @see #isScheduled()
     * @since 0.24
     */
    boolean schedule(String cron);

    /**
     * Re-schedule this job to run at fixed rate, indepedent of whether the preceding execution has finished or not.
     * Has no effect, if the job has already been scheduled and hasn't finished yet.
     *
     * @param fixedRateMs Fixed rate in millis
     * @param initialDelayMs Initial delay in millis
     * @return true, if the job has been re-scheduled.
     * @see #isScheduled()
     * @since 0.24
     */
    boolean scheduleAtFixedRate(long fixedRateMs, long initialDelayMs);

    /**
     * Re-schedule this job to run with fixed interval between executions.
     * Has no effect, if the job has already been scheduled and hasn't finished yet.
     *
     * @param fixedDelayMs Fixed delay in millis to wait after the completion of the preceding execution before starting next
     * @param initialDelayMs Initial delay in millis
     * @return true, if the job has been re-scheduled.
     * @see #isScheduled()
     * @since 0.24
     */
    boolean scheduleWithFixedDelay(long fixedDelayMs, long initialDelayMs);

    /**
     * Re-schedule this job based on the provided schedule.
     * Has no effect, if the job has already been scheduled and hasn't finished yet.
     *
     * @param schedule Schedule object
     * @return true, if the job has been re-scheduled.
     * @see #isScheduled()
     * @since 0.24
     */
    boolean schedule(Schedule schedule);

    /**
     * @return true, if this has been scheduled and has not finished or been cancelled yet
     * @since 0.24
     */
    boolean isScheduled();

    /**
     * @return Schedule, or {@link Optional#empty()}, if {@link #isScheduled()} is false
     * @since 0.24
     */
    Optional<Schedule> getSchedule();
}
