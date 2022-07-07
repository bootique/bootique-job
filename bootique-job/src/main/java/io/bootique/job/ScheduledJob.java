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

import io.bootique.job.scheduler.Trigger;
import io.bootique.job.value.Cron;

/**
 * A job scheduled within the Scheduler, associated with a specific trigger. Job schedule is mutable, and can be altered
 * via "schedule" methods. The job must be canceled before attempting to reschedule.
 *
 * @since 3.0
 */
public interface ScheduledJob {

    String getJobName();

    /**
     * Reschedule this job using the provided cron expression. Has no effect, if the job is already scheduled, so make
     * sure to call {@link #cancel(boolean)} before changing the schedule.
     *
     * @param cron Cron expression
     * @return true, if the job has been re-scheduled.
     */
    boolean schedule(Cron cron);

    /**
     * Reschedule this job to run at fixed rate. Has no effect, if the job is already scheduled, so make
     * sure to call {@link #cancel(boolean)} before changing the schedule.
     *
     * @param fixedRateMs    Fixed rate in millis
     * @param initialDelayMs Initial delay in millis
     * @return true, if the job has been re-scheduled.
     */
    boolean scheduleAtFixedRate(long fixedRateMs, long initialDelayMs);

    /**
     * Reschedule this job to run with fixed interval between executions. Has no effect, if the job is already scheduled,
     * so make sure to call {@link #cancel(boolean)} before changing the schedule.
     *
     * @param fixedDelayMs   Fixed delay in millis to wait after the completion of the preceding execution before
     *                       starting next
     * @param initialDelayMs Initial delay in millis
     * @return true, if the job has been re-scheduled.
     */
    boolean scheduleWithFixedDelay(long fixedDelayMs, long initialDelayMs);

    /**
     * Reschedule this job based on the schedule provided by the trigger. Has no effect, if the job has already been
     * scheduled.
     */
    boolean schedule(Trigger trigger);

    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Returns true if the job has been scheduled and has not been cancelled.
     */
    boolean isScheduled();

    boolean isCancelled();
}
