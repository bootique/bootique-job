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

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.job.Job;
import io.bootique.job.JobDecorator;
import io.bootique.job.JobOutcome;
import io.bootique.job.JobsModule;
import io.bootique.job.Scheduler;
import io.bootique.job.SchedulerModule;
import io.bootique.job.runtime.JobDecorators;
import io.bootique.job.trigger.Trigger;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class SchedulerTriggerStateTransitionsIT {

    final ExecutionRateListener listener = new ExecutionRateListener();

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .modules(new JobsModule(), new SchedulerModule())
            .module(b -> BQCoreModule.extend(b)
                    .setProperty("bq.scheduler.triggers[0].job", "j1")
                    .setProperty("bq.scheduler.triggers[0].trigger", "jt1")
                    .setProperty("bq.scheduler.triggers[0].fixedRate", "100ms"))
            .module(b -> JobsModule.extend(b)
                    .addJob(J1.class)
                    .addDecorator(listener, JobDecorators.EXCEPTIONS_HANDLER_ORDER + 1))
            .createRuntime();

    @Test
    public void schedule_cancel_schedule() throws InterruptedException {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        Trigger t = scheduler.getTrigger("j1", "jt1");
        assertTrue(t.isUnscheduled());

        assertEquals(1, scheduler.scheduleAllTriggers());
        assertTrue(t.isScheduled());

        Thread.sleep(1000);
        listener.assertRateWithinRange(85, 115);

        assertTrue(t.cancel(false));
        assertTrue(t.isCanceled());

        // allow for remaining jobs to complete gracefully
        Thread.sleep(1000);
        listener.reset();

        // measure the rate after everything was stopped
        Thread.sleep(1000);
        listener.assertRateWithinRange(0, 0);
        listener.reset();

        assertTrue(t.schedule());
        assertTrue(t.isScheduled());

        Thread.sleep(1000);
        listener.assertRateWithinRange(85, 115);
    }

    static class ExecutionRateListener implements JobDecorator {

        private final Deque<Execution> executions;

        private volatile double averageRate;

        public ExecutionRateListener() {
            this.executions = new LinkedBlockingDeque<>();
        }

        public void assertRateWithinRange(long lower, long upper) {
            long actual = (long) this.averageRate;
            assertTrue(lower <= actual, () -> "Lower than expected rate: " + actual);
            assertTrue(upper >= actual, () -> "Higher than expected rate: " + actual);
        }

        @Override
        public JobOutcome run(Job delegate, Map<String, Object> params) {
            ExecutionRateListener.Execution previousExecution = executions.peekLast();
            long startedAt = System.currentTimeMillis();

            try {
                return delegate.run(params);
            } finally {
                executions.add(new Execution(System.currentTimeMillis()));
                if (previousExecution != null) {
                    recalculateAverageRate(startedAt - previousExecution.getFinishedAt());
                }
            }
        }

        private synchronized void recalculateAverageRate(long sample) {
            averageRate = rollingAverage(averageRate, sample, executions.size());
        }

        private double rollingAverage(double currentValue, double sample, int totalSamples) {
            currentValue -= currentValue / totalSamples;
            currentValue += sample / totalSamples;
            return currentValue;
        }

        public synchronized void reset() {
            this.executions.clear();
            this.averageRate = 0;
        }

        private static class Execution {
            private final long finishedAt;

            public Execution(long finishedAt) {
                this.finishedAt = finishedAt;
            }

            public long getFinishedAt() {
                return finishedAt;
            }
        }
    }

    static class J1 implements Job {
        @Override
        public JobOutcome run(Map<String, Object> params) {
            return JobOutcome.succeeded();
        }
    }
}
