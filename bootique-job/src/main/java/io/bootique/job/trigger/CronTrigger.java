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
import io.bootique.job.value.Cron;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * @since 3.0
 */
public class CronTrigger extends Trigger {

    private final CronExpression expression;

    public CronTrigger(
            JobRegistry jobRegistry,
            TaskScheduler taskScheduler,
            String jobName,
            String triggerName,
            Map<String, Object> params,
            CronExpression expression) {

        super(jobRegistry, taskScheduler, jobName, triggerName, params);
        this.expression = Objects.requireNonNull(expression);
    }

    /**
     * @deprecated in favor of {@link #getExpression()}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public Cron getCron() {
        return new Cron(expression.toString());
    }

    /**
     * @since 4.0
     */
    public CronExpression getExpression() {
        return expression;
    }

    @Override
    protected Instant nextExecution(SchedulingContext context) {
        Instant timestamp = latestTimestamp(context);
        ZonedDateTime zonedTimestamp = ZonedDateTime.ofInstant(timestamp, context.timeZone());
        ZonedDateTime nextTimestamp = expression.next(zonedTimestamp);
        return (nextTimestamp != null ? nextTimestamp.toInstant() : null);
    }

    Instant latestTimestamp(SchedulingContext context) {
        Instant timestamp = context.lastCompletion();
        if (timestamp != null) {
            Instant scheduled = context.lastScheduledExecution();
            if (scheduled != null && timestamp.isBefore(scheduled)) {
                // Previous task apparently executed too early...
                // Let's simply use the last calculated execution time then,
                // in order to prevent accidental re-fires in the same second.
                timestamp = scheduled;
            }
        } else {
            timestamp = context.now();
        }

        return timestamp;
    }

    @Override
    public String toString() {
        return "cron trigger " + expression;
    }
}
