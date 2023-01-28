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

import io.bootique.job.Job;
import io.bootique.job.ScheduledJob;
import io.bootique.job.trigger.*;
import io.bootique.job.value.Cron;

import java.util.Collections;

/**
 * @since 3.0
 */
public abstract class BaseScheduledJob implements ScheduledJob {

    private final Job job;
    private volatile ScheduledJobState state;

    public BaseScheduledJob(Job job) {
        this.job = job;
        this.state = ScheduledJobState.unscheduled(false);
    }

    @Override
    public String getJobName() {
        return job.getMetadata().getName();
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        this.state = this.state.cancel(mayInterruptIfRunning);
        return isCancelled();
    }

    @Override
    public boolean schedule(Cron cron) {
        Trigger oldTrigger = state.getTrigger();
        JobExec exec = oldTrigger != null
                ? oldTrigger.getExec()
                : new JobExec(getJobName(), Collections.emptyMap());

        return schedule(new CronTrigger(exec, TriggerFactory.generateTriggerName(), cron));
    }

    @Override
    public boolean scheduleAtFixedRate(long fixedRateMs, long initialDelayMs) {
        Trigger oldTrigger = state.getTrigger();
        JobExec exec = oldTrigger != null
                ? oldTrigger.getExec()
                : new JobExec(getJobName(), Collections.emptyMap());

        return schedule(new FixedRateTrigger(exec, TriggerFactory.generateTriggerName(), fixedRateMs, initialDelayMs));
    }

    @Override
    public boolean scheduleWithFixedDelay(long fixedDelayMs, long initialDelayMs) {
        Trigger oldTrigger = state.getTrigger();
        JobExec exec = oldTrigger != null
                ? oldTrigger.getExec()
                : new JobExec(getJobName(), Collections.emptyMap());
        return schedule(new FixedDelayTrigger(exec, TriggerFactory.generateTriggerName(), fixedDelayMs, initialDelayMs));
    }

    @Override
    public boolean schedule(Trigger trigger) {

        if (!isScheduled()) {
            synchronized (this) {
                if (!isScheduled()) {
                    this.state = doSchedule(job, trigger);
                    return true;
                }
            }
        }

        return false;
    }

    protected abstract ScheduledJobState doSchedule(Job job, Trigger trigger);

    @Override
    public boolean isScheduled() {
        return state.isScheduled();
    }

    @Override
    public boolean isCancelled() {
        return state.isCanceled();
    }
}
