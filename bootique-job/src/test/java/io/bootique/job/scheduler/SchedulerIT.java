package io.bootique.job.scheduler;

import io.bootique.BQRuntime;
import io.bootique.job.Job;
import io.bootique.job.JobRegistry;
import io.bootique.job.fixture.ExecutionRateListener;
import io.bootique.job.fixture.ScheduledJob1;
import io.bootique.job.runtime.JobModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class SchedulerIT {

    private ExecutionRateListener listener;
    private BQRuntime runtime;

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Before
    public void setUp() {
        listener = new ExecutionRateListener();
        runtime = testFactory.app("--config=classpath:io/bootique/job/fixture/scheduler_test_triggers.yml")
                .module(JobModule.class)
                .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addListener(listener))
                .createRuntime();
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

        sleep(1000);

        listener.reset();

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
