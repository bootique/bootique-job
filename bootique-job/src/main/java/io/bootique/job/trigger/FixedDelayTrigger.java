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

import io.bootique.job.JobRegistry;
import io.bootique.job.scheduler.TaskScheduler;
import io.bootique.job.scheduler.SchedulingContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * @since 3.0
 */
public class FixedDelayTrigger extends Trigger {

    private final Duration period;
    private final Duration initialDelay;

    public FixedDelayTrigger(
            JobRegistry jobRegistry,
            TaskScheduler taskScheduler,
            String jobName,
            String triggerName,
            Map<String, Object> params,
            Duration period,
            Duration initialDelay) {

        super(jobRegistry, taskScheduler, jobName, triggerName, params);
        this.period = Objects.requireNonNull(period);
        this.initialDelay = initialDelay != null ? initialDelay : Duration.ZERO;
    }

    @Override
    protected Instant nextExecution(SchedulingContext context) {
        Instant lastExecution = context.lastScheduledExecution();
        Instant lastCompletion = context.lastCompletion();
        if (lastExecution == null || lastCompletion == null) {
            Instant instant = context.now();
            Duration initialDelay = this.initialDelay;
            if (initialDelay == null) {
                return instant;
            } else {
                return instant.plus(initialDelay);
            }
        }

        return lastCompletion.plus(period);
    }

    /**
     * @since 4.0
     */
    public Duration getPeriod() {
        return period;
    }

    /**
     * @since 4.0
     */
    public Duration getInitialDelay() {
        return initialDelay;
    }

    /**
     * @deprecated in favor of {@link #getInitialDelay()}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public long getInitialDelayMs() {
        return initialDelay.toMillis();
    }

    /**
     * @deprecated in favor of {@link #getPeriod()}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public long getFixedDelayMs() {
        return period.toMillis();
    }

    @Override
    public String toString() {
        return "fixed delay trigger - delay: " + period + ", initial delay " + initialDelay;
    }
}
