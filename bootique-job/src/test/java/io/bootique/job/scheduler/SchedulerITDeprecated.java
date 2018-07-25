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
import io.bootique.BootiqueException;
import io.bootique.job.Job;
import io.bootique.job.JobRegistry;
import io.bootique.job.fixture.ExecutionRateListener;
import io.bootique.job.fixture.ScheduledJob1;
import io.bootique.job.runtime.JobModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @deprecated since 0.26. This is tests for old TriggerDescriptor configuration, for new configuration please see
 *  {@link SchedulerIT}
 */
@Deprecated
public class SchedulerITDeprecated {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();
    private ExecutionRateListener listener;
    private BQRuntime runtime;

    @Before
    public void before() {
        listener = new ExecutionRateListener();
        runtime = testFactory.app("-c", "classpath:io/bootique/job/fixture/scheduler_test_triggers_deprecated.yml")
                .module(JobModule.class)
                .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addListener(listener))
                .createRuntime();
    }

    @After
    public void after() {
        getScheduler().getScheduledJobs().forEach(ScheduledJobFuture::cancelInterruptibly);
    }

    @Deprecated
    @Test
    public void testScheduler_StartWithNoJobs() {
        Scheduler scheduler = getScheduler();
        assertEquals(0, scheduler.start(Collections.emptyList()));
    }

    @Deprecated
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

    @Deprecated
    @Test
    public void testScheduler_StartAfterPreviousCallToStartFailed() {
        Scheduler scheduler = getScheduler();
        scheduler.start(Collections.emptyList());
        scheduler.start(Collections.singletonList("scheduledjob1"));
        assertTrue(scheduler.isStarted());
    }

    @Deprecated
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

        JobRegistry jobRegistry = runtime.getInstance(JobRegistry.class);
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
        return runtime.getInstance(Scheduler.class);
    }

    private void assertEqualsApprox(long lower, long upper, long actual) {
        assertTrue("Lower than expected rate: " + actual, lower <= actual);
        assertTrue("Higher than expected rate: " + actual, upper >= actual);
    }
}
