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

import io.bootique.job.scheduler.TaskScheduler;
import io.bootique.job.trigger.Trigger;
import io.bootique.job.trigger.TriggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder API to create new triggers in the scheduler. Use one of {@link Scheduler#newCronTrigger(String)},
 * {@link Scheduler#newFixedDelayTrigger(Duration, Duration)}  , {@link Scheduler#newFixedRateTrigger(Duration, Duration)}
 * to start the builder.
 *
 * @since 4.0
 */
public abstract class TriggerBuilder {

    private final Consumer<Trigger> addToSchedulerCallback;
    protected final JobRegistry jobRegistry;
    protected final TaskScheduler taskScheduler;

    private String jobName;
    private String triggerName;
    private Map<String, Object> params;

    protected TriggerBuilder(
            Consumer<Trigger> addToSchedulerCallback,
            JobRegistry jobRegistry,
            TaskScheduler taskScheduler) {

        this.addToSchedulerCallback = Objects.requireNonNull(addToSchedulerCallback);
        this.jobRegistry = Objects.requireNonNull(jobRegistry);
        this.taskScheduler = Objects.requireNonNull(taskScheduler);
    }

    public TriggerBuilder jobName(String name) {
        this.jobName = name;
        return this;
    }

    public TriggerBuilder triggerName(String name) {
        this.triggerName = name;
        return this;
    }

    public TriggerBuilder params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    /**
     * Registers trigger with the underlying scheduler and returns the trigger back to the call. The trigger will
     * be returned in the "unscheduled" state, and will have to be scheduled explicitly if needed.
     */
    public Trigger add() {
        Trigger t = makeTrigger();
        addToSchedulerCallback.accept(t);
        return t;
    }

    protected abstract Trigger makeTrigger();

    protected String createJobName() {
        return Objects.requireNonNull(jobName, "Null 'jobName'");
    }

    protected String createTriggerName() {
        return this.triggerName != null ? this.triggerName : TriggerFactory.generateTriggerName();
    }

    protected Map<String, Object> createParams() {
        return this.params != null ? this.params : Map.of();
    }

}
