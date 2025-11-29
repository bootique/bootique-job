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

import io.bootique.job.trigger.Trigger;
import io.bootique.job.trigger.TriggerContext;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ReschedulingRunnable implements Runnable, ScheduledFuture<Object> {

    private final Runnable delegate;
    private final Trigger trigger;
    private final TriggerContext triggerContext;
    private final ScheduledExecutorService executor;
    private final Object triggerContextMonitor;

    private volatile ScheduledFuture<?> currentFuture;
    private volatile Instant scheduledExecutionTime;

    public ReschedulingRunnable(
            Runnable delegate,
            Trigger trigger,
            Clock clock,
            ScheduledExecutorService executor) {

        this.delegate = delegate;
        this.trigger = trigger;
        this.triggerContext = new TriggerContext(clock);
        this.executor = executor;
        this.triggerContextMonitor = new Object();
    }

    public ScheduledFuture<?> schedule() {
        synchronized (triggerContextMonitor) {

            this.scheduledExecutionTime = trigger.nextExecution(triggerContext);
            if (scheduledExecutionTime == null) {
                return null;
            }

            Duration delay = Duration.between(triggerContext.getClock().instant(), scheduledExecutionTime);
            currentFuture = executor.schedule(this, delay.toNanos(), TimeUnit.NANOSECONDS);
            return this;
        }
    }

    private ScheduledFuture<?> obtainCurrentFuture() {
        return Objects.requireNonNull(currentFuture);
    }

    @Override
    public void run() {
        Instant actualExecutionTime = triggerContext.getClock().instant();
        try {
            delegate.run();
        }
        // TODO: the original never rethrew exceptions, how did they get to us?
        catch (UndeclaredThrowableException ex) {
            throw new RuntimeException(ex.getUndeclaredThrowable());
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }

        Instant completionTime = triggerContext.getClock().instant();

        synchronized (triggerContextMonitor) {
            Objects.requireNonNull(scheduledExecutionTime, "No scheduled execution");
            triggerContext.update(scheduledExecutionTime, actualExecutionTime, completionTime);
            if (!obtainCurrentFuture().isCancelled()) {
                schedule();
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
        synchronized (this.triggerContextMonitor) {
            curr = obtainCurrentFuture();
        }
        return curr.get(timeout, unit);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        ScheduledFuture<?> curr;
        synchronized (this.triggerContextMonitor) {
            curr = obtainCurrentFuture();
        }
        return curr.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed other) {
        if (this == other) {
            return 0;
        }
        long diff = getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
        return diff == 0 ? 0 : (diff < 0 ? -1 : 1);
    }
}
