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
import io.bootique.job.runnable.JobResult;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

class DefaultScheduledJobFuture implements ScheduledJobFuture {

    private final String jobName;
    private final Function<Trigger, JobFuture> scheduler;

    private Trigger trigger;
    private JobFuture future;
    private boolean cancelled;

    public DefaultScheduledJobFuture(String jobName, Function<Trigger, JobFuture> scheduler) {
        this.jobName = jobName;
        this.scheduler = scheduler;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    // explicit fool-proof synchronization to avoid double-scheduling
    @Override
    public synchronized boolean schedule(Trigger trigger) {

        if (isScheduled()) {
            return false;
        }

        this.future = scheduler.apply(trigger);
        this.trigger = trigger;
        this.cancelled = false;
        return true;
    }

    @Override
    public Trigger getTrigger() {
        checkScheduled();
        return trigger;
    }

    @Override
    public boolean isScheduled() {
        return future != null;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {

        if (cancelled) {
            return false;
        }

        JobFuture future = this.future;
        if (future == null) {
            return false;
        }

        boolean cancelled = future.cancel(mayInterruptIfRunning);
        if (cancelled) {
            this.trigger = null;
            this.future = null;
        }

        this.cancelled = cancelled;
        return cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return future != null && future.isDone();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        checkScheduled();
        return future.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
        checkScheduled();
        return future.compareTo(o);
    }

    @Override
    public JobResult get() {
        checkScheduled();
        return future.get();
    }

    @Override
    public JobResult get(long timeout, TimeUnit unit) {
        checkScheduled();
        return future.get(timeout, unit);
    }

    private void checkScheduled() {
        if (!isScheduled()) {
            throw new IllegalStateException("Job is not scheduled: " + jobName);
        }
    }
}
