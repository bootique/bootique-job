package io.bootique.job.scheduler;

import java.util.Optional;

public interface ScheduledJob {

    String getJobName();

    boolean schedule(String cron);

    boolean scheduleAtFixedRate(long fixedRateMs, long initialDelayMs);

    boolean scheduleWithFixedDelay(long fixedDelayMs, long initialDelayMs);

    boolean schedule(Schedule schedule);

    boolean isScheduled();

    Optional<Schedule> getSchedule();

    /**
     * Cancel a scheduled job execution.
     *
     * This attempt will fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason (i.e. if the job is unknown to scheduler
     * or if the provided trigger descriptor has never been scheduled).
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     *          task should be interrupted; otherwise, in-progress tasks are allowed
     *          to complete
     * @return true if the scheduled job execution has been canceled
     * @since 0.24
     */
    boolean cancel(boolean mayInterruptIfRunning);
}
