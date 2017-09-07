package io.bootique.job.scheduler;

import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;

import java.util.Optional;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

class DefaultScheduledJobFuture implements ScheduledJobFuture {

    private final String jobName;
    private final Function<Schedule, JobFuture> scheduler;
    private Optional<Schedule> schedule;
    private Optional<JobFuture> futureOptional;
    private boolean cancelled;

    public DefaultScheduledJobFuture(String jobName, Function<Schedule, JobFuture> scheduler) {
        this.jobName = jobName;
        this.scheduler = scheduler;
        this.schedule = Optional.empty();
        this.futureOptional = Optional.empty();
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public boolean schedule(String cron) {
        return schedule(Schedule.cron(cron));
    }

    @Override
    public boolean scheduleAtFixedRate(long fixedRateMs, long initialDelayMs) {
        return schedule(Schedule.fixedRate(fixedRateMs, initialDelayMs));
    }

    @Override
    public boolean scheduleWithFixedDelay(long fixedDelayMs, long initialDelayMs) {
        return schedule(Schedule.fixedDelay(fixedDelayMs, initialDelayMs));
    }

    // explicit fool-proof synchronization to avoid double-scheduling
    @Override
    public synchronized boolean schedule(Schedule schedule) {
        if (isScheduled()) {
            return false;
        }
        this.schedule = Optional.of(schedule);
        this.futureOptional = Optional.of(scheduler.apply(schedule));
        this.cancelled = false;
        return true;
    }

    @Override
    public Optional<Schedule> getSchedule() {
        return schedule;
    }

    @Override
    public boolean isScheduled() {
        return futureOptional.isPresent();
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (!isScheduled()) {
            return false;
        }
        boolean cancelled = futureOptional.get().cancel(mayInterruptIfRunning);
        if (cancelled) {
            schedule = Optional.empty();
            futureOptional = Optional.empty();
        }
        this.cancelled = cancelled;
        return cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return isScheduled() && futureOptional.get().isDone();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        assertIsScheduled();
        return futureOptional.get().getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
        assertIsScheduled();
        return futureOptional.get().compareTo(o);
    }

    @Override
    public JobResult get() {
        assertIsScheduled();
        return futureOptional.get().get();
    }

    @Override
    public JobResult get(long timeout, TimeUnit unit) {
        assertIsScheduled();
        return futureOptional.get().get(timeout, unit);
    }

    private void assertIsScheduled() {
        if (!isScheduled()) {
            throw new IllegalStateException("Job is not scheduled: " + jobName);
        }
    }
}
