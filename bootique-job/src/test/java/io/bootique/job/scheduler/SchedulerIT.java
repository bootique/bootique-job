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
import io.bootique.job.JobRegistry;
import io.bootique.job.fixture.ExecutionRateListener;
import io.bootique.job.fixture.ScheduledJob1;
import io.bootique.job.runtime.JobModule;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

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
        assertTrue(scheduledJob.getSchedule().isPresent());
        assertEquals("fixedRateMs: 100", scheduledJob.getSchedule().get().getDescription());

        JobRegistry jobRegistry = app.getInstance(JobRegistry.class);
        Job job = jobRegistry.getJob(scheduledJob.getJobName());
        assertNotNull(job);

        Thread.sleep(1000);

        assertEqualsApprox(85, 115, listener.getAverageRate());

        assertTrue(scheduledJob.cancel(false));
        assertFalse(scheduledJob.isScheduled());
        assertFalse(scheduledJob.getSchedule().isPresent());

        // allow for remaining jobs to complete gracefully
        // (Future.cancel() does not wait for actual completion)
        Thread.sleep(1000);

        listener.reset();

        // wait for a bit to ensure that no jobs are running in the background
        Thread.sleep(1000);

        assertEquals(0, listener.getAverageRate());

        assertTrue(scheduledJob.scheduleAtFixedRate(50, 0));
        assertTrue(scheduledJob.isScheduled());
        assertTrue(scheduledJob.getSchedule().isPresent());
        assertEquals("fixedRateMs: 50", scheduledJob.getSchedule().get().getDescription());

        Thread.sleep(1000);

        assertEqualsApprox(40, 60, listener.getAverageRate());
    }

    private Scheduler getScheduler() {
        return app.getInstance(Scheduler.class);
    }

    private void assertEqualsApprox(long lower, long upper, long actual) {
        assertTrue(lower <= actual, () -> "Lower than expected rate: " + actual);
        assertTrue(upper >= actual, () -> "Higher than expected rate: " + actual);
    }
}
