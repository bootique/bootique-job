package io.bootique.job.scheduler;

import io.bootique.job.runnable.JobFuture;

import java.util.Optional;

public interface ScheduledJobFuture extends JobFuture {

    boolean schedule(String cron);

    boolean scheduleAtFixedRate(long fixedRateMs, long initialDelayMs);

    boolean scheduleWithFixedDelay(long fixedDelayMs, long initialDelayMs);

    boolean schedule(Schedule schedule);

    boolean isScheduled();

    Optional<Schedule> getSchedule();
}
