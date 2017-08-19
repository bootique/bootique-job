package io.bootique.job.scheduler;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.job.Job;
import io.bootique.job.JobRegistry;
import io.bootique.job.fixture.ExecutionRateListener;
import io.bootique.job.fixture.SchedulerTestModule;
import io.bootique.job.runtime.JobModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class SchedulerTest {

    private ExecutionRateListener listener;
    private BQRuntime runtime;

    @Before
    public void setUp() {
        listener = new ExecutionRateListener();
        runtime = Bootique.app("--config=classpath:io/bootique/job/fixture/scheduler_test_triggers.yml")
                .module(JobModule.class)
                .module(new SchedulerTestModule(Collections.singleton(listener)))
                .createRuntime();
    }

    @After
    public void tearDown() {
        runtime.shutdown();
    }

    @Test
    public void testScheduler_Reschedule() {
        Scheduler scheduler = runtime.getInstance(Scheduler.class);

        int jobCount = scheduler.start();
        assertEquals(1, jobCount);

        Collection<ScheduledJob> scheduledJobs = scheduler.getScheduledJobs();
        assertEquals(1, scheduledJobs.size());

        ScheduledJob scheduledJob = scheduledJobs.iterator().next();
        assertEquals("scheduledjob1", scheduledJob.getJobName());
        assertTrue(scheduledJob.isScheduled());
        assertTrue(scheduledJob.getSchedule().isPresent());
        assertEquals("fixedRateMs: 100", scheduledJob.getSchedule().get().getDescription());

        JobRegistry jobRegistry = runtime.getInstance(JobRegistry.class);
        Job job = jobRegistry.getJob(scheduledJob.getJobName());
        assertNotNull(job);

        sleep(1000);

        assertEqualsApprox(85, 115, listener.getAverageRate());

        assertTrue(scheduledJob.cancel(false));
        assertFalse(scheduledJob.isScheduled());
        assertFalse(scheduledJob.getSchedule().isPresent());

        listener.reset();

        sleep(1000);
        assertEquals(0, listener.getAverageRate());

        assertTrue(scheduledJob.scheduleAtFixedRate(50, 0));
        assertTrue(scheduledJob.isScheduled());
        assertTrue(scheduledJob.getSchedule().isPresent());
        assertEquals("fixedRateMs: 50", scheduledJob.getSchedule().get().getDescription());

        sleep(1000);

        assertEqualsApprox(40, 60, listener.getAverageRate());
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpectedly interrupted", e);
        }
    }

    private void assertEqualsApprox(long lower, long upper, long actual) {
        assertTrue("Lower than expected rate: " + actual, lower <= actual);
        assertTrue("Higher than expected rate: " + actual, upper >= actual);
    }
}
