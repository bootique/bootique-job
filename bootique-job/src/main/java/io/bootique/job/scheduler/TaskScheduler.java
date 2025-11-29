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

import org.springframework.scheduling.Trigger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @since 4.0
 */
public class TaskScheduler implements AutoCloseable {

    private final Clock clock;
    private final ScheduledExecutorService executor;

    public TaskScheduler(int poolSize, String threadNamePrefix) {

        this.clock = java.time.Clock.systemDefaultZone();

        ThreadFactory threadFactory = createThreadFactory(threadNamePrefix);
        this.executor = createExecutor(poolSize, threadFactory);

        if (this.executor instanceof ScheduledThreadPoolExecutor threadPoolExecutor) {
            threadPoolExecutor.setCorePoolSize(poolSize);
        }
    }

    @Override
    public void close() {
        for (Runnable remainingTask : executor.shutdownNow()) {
            cancelRemainingTask(remainingTask);
        }
    }

    private ThreadFactory createThreadFactory(String threadNamePrefix) {
        return Thread.ofVirtual().name(threadNamePrefix, 0).factory();
    }

    private ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory) {
        return new ScheduledThreadPoolExecutor(poolSize, threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    private void cancelRemainingTask(Runnable task) {
        if (task instanceof Future<?> future) {
            future.cancel(true);
        }
    }

    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        return new ReschedulingRunnable(task, trigger, clock, executor).schedule();
    }

    public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
        Duration delay = Duration.between(clock.instant(), startTime);
        return executor.schedule(task, TimeUnit.NANOSECONDS.convert(delay), TimeUnit.NANOSECONDS);
    }
}
