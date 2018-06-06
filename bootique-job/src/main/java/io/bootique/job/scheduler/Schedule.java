/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
