package io.bootique.job.scheduler;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

public class Schedule {

    public static Schedule cron(String cron) {
        return new Schedule(new CronTrigger(cron), "cron: " + cron);
    }

    public static Schedule fixedDelay(long fixedDelayMs, long initialDelayMs) {
        PeriodicTrigger pt = new PeriodicTrigger(fixedDelayMs);
        pt.setFixedRate(false);
        pt.setInitialDelay(initialDelayMs);
        return new Schedule(pt, "fixedDelayMs: " + fixedDelayMs);
    }

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

    public String getDescription() {
        return description;
    }
}
