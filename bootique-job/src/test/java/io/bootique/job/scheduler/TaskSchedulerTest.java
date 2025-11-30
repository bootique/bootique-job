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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskSchedulerTest {

    TaskScheduler scheduler = new TaskScheduler(Clock.systemDefaultZone(), 2, "test-scheduler");

    @AfterEach
    public void afterEach() {
        scheduler.close();
    }

    @Test
    public void schedule() throws InterruptedException {
        TestRunnable runnable = new TestRunnable();
        scheduler.schedule(runnable, c -> Instant.now().plusMillis(50));

        Thread.sleep(300);
        assertTrue(runnable.counter > 2, () -> String.valueOf(runnable.counter));
    }

    @Test
    public void scheduleDying() throws InterruptedException {
        TestDyingRunnable runnable = new TestDyingRunnable();
        scheduler.schedule(runnable, c -> Instant.now().plusMillis(50));

        Thread.sleep(300);
        assertTrue(runnable.counter > 2, () -> String.valueOf(runnable.counter));
    }

    @Test
    public void scheduleCancel() throws InterruptedException {
        TestRunnable runnable = new TestRunnable();
        Future<?> f = scheduler.schedule(runnable, c -> Instant.now().plusMillis(50));

        Thread.sleep(200);
        int c1 = runnable.counter;
        assertTrue(c1 > 1, () -> String.valueOf(c1));

        Thread.sleep(200);
        f.cancel(true);
        Thread.sleep(200);

        int c2 = runnable.counter;
        assertTrue(c2 > c1, () -> String.valueOf(c2));

        Thread.sleep(200);
        int c3 = runnable.counter;
        assertSame(c2, c3, "A task wasn't canceled");
    }

    static class TestRunnable implements Runnable {
        int counter;

        @Override
        public void run() {
            counter++;
        }
    }

    static class TestDyingRunnable implements Runnable {
        int counter;

        @Override
        public void run() {
            counter++;
            throw new RuntimeException("Test exception");
        }
    }
}
