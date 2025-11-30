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

package io.bootique.job.trigger;

import io.bootique.job.Job;
import io.bootique.job.JobRegistry;
import io.bootique.job.scheduler.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * Defines execution schedule for a given job.
 *
 * @since 3.0
 */
public abstract class Trigger {

    enum TriggerSchedulingState {
        unscheduled, scheduled, canceled
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Trigger.class);

    private final JobRegistry jobRegistry;
    private final TaskScheduler taskScheduler;

    private final String jobName;
    private final String triggerName;
    private final Map<String, Object> params;

    private volatile TriggerSchedulingState state;
    private volatile Future<?> future;

    public Trigger(
            JobRegistry jobRegistry,
            TaskScheduler taskScheduler,
            String jobName,
            String triggerName,
            Map<String, Object> params) {
        this.jobRegistry = Objects.requireNonNull(jobRegistry);
        this.taskScheduler = Objects.requireNonNull(taskScheduler);
        this.jobName = Objects.requireNonNull(jobName);

        // clone params passed to us, as we expect this map to be mutated when passed through a chain of decorators
        this.params = new HashMap<>(Objects.requireNonNull(params));
        this.triggerName = Objects.requireNonNull(triggerName);
        this.state = TriggerSchedulingState.unscheduled;
    }

    /**
     * Schedules the job pointed to by trigger. Returns false if the job is already scheduled.
     *
     * @since 4.0
     */
    public boolean schedule() {
        if (!isScheduled()) {
            synchronized (this) {
                if (!isScheduled()) {
                    LOGGER.info("Will schedule '{}'.. ({})", jobName, this);

                    Job job = jobRegistry.getJob(jobName);

                    this.future = taskScheduler.schedule(() -> job.run(params), this::nextExecution);
                    this.state = TriggerSchedulingState.scheduled;
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @since 4.0
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isScheduled()) {
            synchronized (this) {
                if (isScheduled()) {
                    boolean wasCanceled = this.future.cancel(mayInterruptIfRunning);
                    this.future = null;
                    this.state = TriggerSchedulingState.canceled;
                    return wasCanceled;
                }
            }
        }

        return false;
    }

    /**
     * Determines the next execution time based on the internal trigger logic and provided context.
     */
    protected abstract Instant nextExecution(TriggerContext context);

    /**
     * @deprecated in favor of {@link #getTriggerName()}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public String getName() {
        return triggerName;
    }

    /**
     * Returns the name of the trigger. In combination with "jobName", forms a unique fully-qualified name within the
     * Scheduler, so it must be unique within the Scheduler for a given job.
     *
     * @since 4.0
     */
    public String getTriggerName() {
        return triggerName;
    }

    /**
     * @since 4.0
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @since 4.0
     */
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * @since 4.0
     */
    public boolean isUnscheduled() {
        return state == TriggerSchedulingState.unscheduled;
    }

    /**
     * @since 4.0
     */
    public boolean isScheduled() {
        return state == TriggerSchedulingState.scheduled;
    }

    /**
     * @since 4.0
     */
    public boolean isCanceled() {
        return state == TriggerSchedulingState.canceled;
    }
}
