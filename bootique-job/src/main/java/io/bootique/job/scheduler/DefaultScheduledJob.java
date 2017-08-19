package io.bootique.job.scheduler;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

class DefaultScheduledJob implements ScheduledJob {

    private final Function<Schedule, ScheduledFuture<?>> scheduler;
    private final String jobName;
    private Optional<Schedule> schedule;
    private Optional<ScheduledFuture<?>> futureOptional;

    public DefaultScheduledJob(String jobName, Function<Schedule, ScheduledFuture<?>> scheduler) {
        this.scheduler = scheduler;
        this.jobName = jobName;
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

    @Override
    public boolean schedule(Schedule schedule) {
        if (isScheduled()) {
            return false;
        }
        this.schedule = Optional.of(schedule);
        this.futureOptional = Optional.of(scheduler.apply(schedule));
        return true;
    }

    @Override
    public Optional<Schedule> getSchedule() {
        return isScheduled() ? schedule : Optional.empty();
    }

    @Override
    public boolean isScheduled() {
        return futureOptional.isPresent() && isStarted(futureOptional.get());
    }

    private boolean isStarted(Future<?> future) {
        return (!future.isDone() && !future.isCancelled());
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return isScheduled() && futureOptional.get().cancel(mayInterruptIfRunning);
    }
}
