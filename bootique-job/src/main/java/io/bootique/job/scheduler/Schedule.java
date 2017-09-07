package io.bootique.job.scheduler;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @since 0.24
 */
public class Schedule {

    /**
     * Create a schedule based on cron expression
     *
     * @param cron Cron expression
     * @return Schedule
     * @since 0.24
     */
    public static Schedule cron(String cron) {
        return new Schedule(new CronTrigger(cron), "cron: " + cron);
    }

    /**
     * Create a schedule with fixed delay between job executions.
     *
     * @param fixedDelayMs Fixed delay in millis
     * @param initialDelayMs Initial delay in millis
     * @return Schedule
     * @since 0.24
     * @see ScheduledJobFuture#scheduleWithFixedDelay(long, long)
     */
    public static Schedule fixedDelay(long fixedDelayMs, long initialDelayMs) {
        PeriodicTrigger pt = new PeriodicTrigger(fixedDelayMs);
        pt.setFixedRate(false);
        pt.setInitialDelay(initialDelayMs);
        return new Schedule(pt, "fixedDelayMs: " + fixedDelayMs);
    }

    /**
     * Create a schedule with fixed rate of launching job executions.
     *
     * @param fixedRateMs Fixed rate in millis
     * @param initialDelayMs Initial delay in millis
     * @return Schedule
     * @since 0.24
     * @see ScheduledJobFuture#scheduleAtFixedRate(long, long)
     */
    public static Schedule fixedRate(long fixedRateMs, long initialDelayMs) {
        PeriodicTrigger pt = new PeriodicTrigger(fixedRateMs);
        pt.setFixedRate(true);
        pt.setInitialDelay(initialDelayMs);
        return new Schedule(pt, "fixedRateMs: " + fixedRateMs);
    }

    private final Trigger trigger;
    private final String description;

    private Schedule(Trigger trigger, String description) {
        this.trigger = trigger;
        this.description = description;
    }

    // package-private method
    Trigger getTrigger() {
        return trigger;
    }

    /**
     * @return Textual (human-readable) description of this schedule
     * @since 0.24
     */
    public String getDescription() {
        return description;
    }
}
