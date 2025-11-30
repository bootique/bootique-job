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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A combination of a Runnable and a Future that reschedules itself upon each run completion according to the internal
 * Trigger rules.
 */
class ReschedulingRunnable implements Runnable, Future<Object> {

    private final Runnable delegate;
    private final Schedule schedule;
    private final SchedulingContext context;
    private final ScheduledExecutorService executor;
    private final Object triggerContextMonitor;

    private volatile ScheduledFuture<?> currentFuture;
    private volatile Instant scheduledExecutionTime;

    public ReschedulingRunnable(
            Runnable delegate,
            Schedule schedule,
            Clock clock,
            ScheduledExecutorService executor) {

        this.delegate = delegate;
        this.schedule = schedule;
        this.context = new SchedulingContext(clock);
        this.executor = executor;
        this.triggerContextMonitor = new Object();
    }

    public void schedule() {
        synchronized (triggerContextMonitor) {
            this.scheduledExecutionTime = schedule.nextExecution(context);
            if (scheduledExecutionTime != null) {
                Duration delay = Duration.between(context.now(), scheduledExecutionTime);
                currentFuture = executor.schedule(this, delay.toNanos(), TimeUnit.NANOSECONDS);
            }
        }
    }

    private ScheduledFuture<?> obtainCurrentFuture() {
        return Objects.requireNonNull(currentFuture);
    }

    @Override
    public void run() {
        try {
            delegate.run();
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        } finally {

            // Reschedule the next run. Don't die on errors. Only an explicit cancellation (or the trigger deciding to
            // stop) would stop rescheduling

            // Would've been great to create a new ReschedulingRunnable here instead of doing synchronized mutation
            // of this one, but it won't work, as _this_ future is referenced by the calling trigger between runs for
            // the purpose of cancellation

            synchronized (triggerContextMonitor) {
                Objects.requireNonNull(scheduledExecutionTime, "No scheduled execution");

                context.update(scheduledExecutionTime, context.now());
                if (!obtainCurrentFuture().isCancelled()) {
                    schedule();
                }
            }
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (triggerContextMonitor) {
            return obtainCurrentFuture().cancel(mayInterruptIfRunning);
        }
    }

    @Override
    public boolean isCancelled() {
        synchronized (triggerContextMonitor) {
            return obtainCurrentFuture().isCancelled();
        }
    }

    @Override
    public boolean isDone() {
        synchronized (triggerContextMonitor) {
            return obtainCurrentFuture().isDone();
        }
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        ScheduledFuture<?> f;
        synchronized (triggerContextMonitor) {
            f = obtainCurrentFuture();
        }
        return f.get();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        ScheduledFuture<?> curr;
        synchronized (triggerContextMonitor) {
            curr = obtainCurrentFuture();
        }
        return curr.get(timeout, unit);
    }
}
