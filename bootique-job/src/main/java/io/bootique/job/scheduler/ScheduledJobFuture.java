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

package io.bootique.job.scheduler;

import io.bootique.job.runnable.JobFuture;
import io.bootique.job.value.Cron;

import java.util.Collections;
import java.util.Map;

public interface ScheduledJobFuture extends JobFuture {

    /**
     * Re-schedule this job based on the provided cron expression.
     * Has no effect, if the job has already been scheduled and hasn't finished yet.
     *
     * @param cron Cron expression
     * @return true, if the job has been re-scheduled.
     * @see #isScheduled()
     */
    default boolean schedule(Cron cron) {
        Trigger oldTrigger = isScheduled() ? getTrigger() : null;
        String jobName = oldTrigger != null ? oldTrigger.getJobName() : getJobName();
        Map<String, Object> params = oldTrigger != null ? oldTrigger.getParams() : Collections.emptyMap();
        return schedule(new CronTrigger(jobName, TriggerFactory.generateTriggerName(), params, cron));
    }

    /**
     * Re-schedule this job to run at fixed rate, indepedent of whether the preceding execution has finished or not.
     * Has no effect, if the job has already been scheduled and hasn't finished yet.
     *
     * @param fixedRateMs    Fixed rate in millis
     * @param initialDelayMs Initial delay in millis
     * @return true, if the job has been re-scheduled.
     * @see #isScheduled()
     */
    default boolean scheduleAtFixedRate(long fixedRateMs, long initialDelayMs) {
        Trigger oldTrigger = isScheduled() ? getTrigger() : null;
        String jobName = oldTrigger != null ? oldTrigger.getJobName() : getJobName();
        Map<String, Object> params = oldTrigger != null ? oldTrigger.getParams() : Collections.emptyMap();
        return schedule(new FixedRateTrigger(jobName, TriggerFactory.generateTriggerName(), params, fixedRateMs, initialDelayMs));
    }

    /**
     * Re-schedule this job to run with fixed interval between executions.
     * Has no effect, if the job has already been scheduled and hasn't finished yet.
     *
     * @param fixedDelayMs   Fixed delay in millis to wait after the completion of the preceding execution before starting next
     * @param initialDelayMs Initial delay in millis
     * @return true, if the job has been re-scheduled.
     * @see #isScheduled()
     */
    default boolean scheduleWithFixedDelay(long fixedDelayMs, long initialDelayMs) {
        Trigger oldTrigger = isScheduled() ? getTrigger() : null;
        String jobName = oldTrigger != null ? oldTrigger.getJobName() : getJobName();
        Map<String, Object> params = oldTrigger != null ? oldTrigger.getParams() : Collections.emptyMap();
        return schedule(new FixedDelayTrigger(jobName, TriggerFactory.generateTriggerName(), params, fixedDelayMs, initialDelayMs));
    }

    /**
     * Reschedule this job based on the schedule provided by the trigger. Has no effect, if the job has already been
     * scheduled.
     */
    boolean schedule(Trigger trigger);

    /**
     * Retruns true, if this has been scheduled and has not finished or been cancelled yet
     */
    boolean isScheduled();

    /**
     * Returns the trigger associated with this future, or throws if {@link #isScheduled()} is false
     */
    Trigger getTrigger();
}
