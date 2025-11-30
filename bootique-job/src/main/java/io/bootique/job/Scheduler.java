/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.job;

import io.bootique.job.trigger.Trigger;

import java.time.Duration;
import java.util.List;

public interface Scheduler {

    /**
     * Returns a builder object for a new cron trigger associated with this scheduler.
     *
     * @since 4.0
     */
    TriggerBuilder newCronTrigger(String cron);

    /**
     * Returns a builder object for a new fixed delay trigger associated with this scheduler.
     *
     * @since 4.0
     */
    TriggerBuilder newFixedDelayTrigger(Duration period, Duration initialDelay);

    /**
     * Returns a builder object for a new fixed rate trigger associated with this scheduler.
     *
     * @since 4.0
     */
    TriggerBuilder newFixedRateTrigger(Duration period, Duration initialDelay);

    /**
     * Creates a builder for a custom job execution.
     *
     * @since 4.0
     */
    ExecBuilder newExecution();

    /**
     * @since 3.0
     * @deprecated in favor of {@link #newExecution()}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default ExecBuilder runBuilder() {
        return newExecution();
    }

    /**
     * @deprecated in favor of {@link #scheduleAllTriggers()}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default int start() {
        return scheduleAllTriggers();
    }

    /**
     * @deprecated in favor of {@link #scheduleTriggers(String)}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default int start(List<String> jobNames) {
        int i = 0;
        for (String j : jobNames) {
            i += scheduleTriggers(j);
        }

        return i;
    }

    /**
     * Schedules all existing preconfigured triggers. Can be called multiple times, and will only schedule the triggers
     * that are either unscheduled or canceled.
     *
     * @return the number of newly scheduled triggers
     * @since 4.0
     */
    int scheduleAllTriggers();

    /**
     * Schedules preconfigured triggers associated with the specified job. Can be called multiple times, and will
     * only schedule the triggers that are either unscheduled or canceled.
     *
     * @return the number of newly scheduled triggers
     * @since 4.0
     */
    int scheduleTriggers(String jobName);

    /**
     * Tries to schedule an existing preconfigured trigger.
     *
     * @since 4.0
     */
    boolean scheduleTrigger(String jobName, String triggerName);

    /**
     * Cancels all existing preconfigured triggers. Can be called multiple times, and will only cancel the triggers
     * that are scheduled.
     *
     * @return the number of newly scheduled triggers
     * @since 4.0
     */
    int cancelAllTriggers(boolean mayInterruptIfRunning);

    /**
     * Schedules preconfigured triggers associated with the specified job. Can be called multiple times, and will
     * only cancel the triggers that are scheduled.
     *
     * @return the number of newly scheduled triggers
     * @since 4.0
     */
    int cancelTriggers(String jobName, boolean mayInterruptIfRunning);

    /**
     * Tries to cancel an existing preconfigured trigger.
     *
     * @since 4.0
     */
    boolean cancelTrigger(String jobName, String triggerName, boolean mayInterruptIfRunning);

    /**
     * Removes all existing preconfigured triggers. Scheduled triggers will be canceled before removal.
     *
     * @return the number of removed triggers
     * @since 4.0
     */
    int removeAllTriggers();

    /**
     * Removes preconfigured triggers associated with the specified job. Scheduled triggers will be canceled before removal.
     *
     * @return the number of removed triggers
     * @since 4.0
     */
    int removeTriggers(String jobName);

    /**
     * Tries to remove an existing preconfigured trigger. Scheduled triggers will be canceled before removal.
     *
     * @since 4.0
     */
    boolean removeTrigger(String jobName, String triggerName);

    /**
     * @return true, if the scheduler has been started
     * @deprecated started state is no longer meaningful
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default boolean isStarted() {
        return true;
    }

    /**
     * Returns all triggers for all jobs.
     *
     * @since 4.0
     */
    List<Trigger> getAllTriggers();

    /**
     * Returns all triggers for a given job.
     *
     * @since 4.0
     */
    List<Trigger> getTriggers(String jobName);

    /**
     * Returns a non-null Trigger object for the provided job and trigger name. Throws an exception if there's no
     * trigger matching the name.
     *
     * @since 4.0
     */
    Trigger getTrigger(String jobName, String triggerName);
}
