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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.BootiqueException;
import io.bootique.job.Job;
import io.bootique.job.JobListener;
import io.bootique.job.JobModule;
import io.bootique.job.JobRegistry;
import io.bootique.job.fixture.ScheduledJob1;
import io.bootique.job.runnable.JobResult;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class SchedulerIT {

    final ExecutionRateListener listener = new ExecutionRateListener();

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app("-c", "classpath:io/bootique/job/fixture/scheduler_test_triggers.yml")
            .module(JobModule.class)
            .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addListener(listener))
            .createRuntime();


    @BeforeEach
    public void before() {
        listener.reset();
    }

    @AfterEach
    public void after() {
        getScheduler().getScheduledJobs().forEach(ScheduledJobFuture::cancelInterruptibly);
    }

    @Test
    public void testScheduler_StartWithNoJobs() {
        Scheduler scheduler = getScheduler();
        assertEquals(0, scheduler.start(Collections.emptyList()));
    }

    @Test
    public void testScheduler_StartWithUnknownJob_Exception() {
        Scheduler scheduler = getScheduler();

        try {
            scheduler.start(Collections.singletonList("bogusjob123"));
            fail("Exception excepted on invalid job name");
        } catch (BootiqueException e) {
            assertTrue(e.getMessage().equals("Unknown job: bogusjob123"));
        }
    }

    @Test
    public void testScheduler_StartAfterPreviousCallToStartFailed() {
        Scheduler scheduler = getScheduler();
        scheduler.start(Collections.emptyList());
        scheduler.start(Collections.singletonList("scheduledjob1"));
        assertTrue(scheduler.isStarted());
    }

    @Test
    public void testScheduler_Reschedule() throws InterruptedException {
        Scheduler scheduler = getScheduler();

        int jobCount = scheduler.start();
        assertEquals(1, jobCount);

        Collection<ScheduledJobFuture> scheduledJobs = scheduler.getScheduledJobs();
        assertEquals(1, scheduledJobs.size());

        ScheduledJobFuture scheduledJob = scheduledJobs.iterator().next();
        assertEquals("scheduledjob1", scheduledJob.getJobName());
        assertTrue(scheduledJob.isScheduled());
        assertTrue(scheduledJob.getTrigger().isPresent());
        assertEquals("fixed rate trigger 100 ms", scheduledJob.getTrigger().get().toString());

        JobRegistry jobRegistry = app.getInstance(JobRegistry.class);
        Job job = jobRegistry.getJob(scheduledJob.getJobName());
        assertNotNull(job);

        Thread.sleep(1000);

        assertWithinRange(85, 115, listener.getAverageRate());

        assertTrue(scheduledJob.cancel(false));
        assertFalse(scheduledJob.isScheduled());
        assertFalse(scheduledJob.getTrigger().isPresent());

        // allow for remaining jobs to complete gracefully
        // (Future.cancel() does not wait for actual completion)
        Thread.sleep(1000);

        listener.reset();

        // wait for a bit to ensure that no jobs are running in the background
        Thread.sleep(1000);

        assertEquals(0, listener.getAverageRate());

        assertTrue(scheduledJob.schedule(new FixedRateTrigger("scheduledjob1", "XXXX", Collections.emptyMap(), 50, 0)));
        assertTrue(scheduledJob.isScheduled());
        assertTrue(scheduledJob.getTrigger().isPresent());
        assertEquals("fixed rate trigger 50 ms", scheduledJob.getTrigger().get().toString());

        Thread.sleep(1000);

        assertWithinRange(40, 60, listener.getAverageRate());
    }

    private Scheduler getScheduler() {
        return app.getInstance(Scheduler.class);
    }

    private void assertWithinRange(long lower, long upper, long actual) {
        assertTrue(lower <= actual, () -> "Lower than expected rate: " + actual);
        assertTrue(upper >= actual, () -> "Higher than expected rate: " + actual);
    }

    static class ExecutionRateListener implements JobListener {

        private final Deque<Execution> executions;

        private volatile double averageRate;

        public ExecutionRateListener() {
            this.executions = new LinkedBlockingDeque<>();
        }

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry) {
            ExecutionRateListener.Execution previousExecution = executions.peekLast();
            long startedAt = System.currentTimeMillis();

            onFinishedCallbackRegistry.accept(result -> {
                executions.add(new Execution(System.currentTimeMillis()));
                if (previousExecution != null) {
                    recalculateAverageRate(startedAt - previousExecution.getFinishedAt());
                }
            });
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

        public long getAverageRate() {
            return (long) averageRate;
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
}
