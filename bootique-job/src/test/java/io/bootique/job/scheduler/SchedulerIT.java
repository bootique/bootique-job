package io.bootique.job.scheduler;

import io.bootique.BQRuntime;
import io.bootique.job.Job;
import io.bootique.job.JobRegistry;
import io.bootique.job.fixture.ExecutionRateListener;
import io.bootique.job.fixture.ScheduledJob1;
import io.bootique.job.fixture.ScheduledJob2;
import io.bootique.job.runtime.JobModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static io.bootique.job.Utils.sleep;
import static org.junit.Assert.*;

public class SchedulerIT {

    private ExecutionRateListener listener;
    private BQRuntime runtime;

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Before
    public void before() {
        listener = new ExecutionRateListener();
        runtime = testFactory.app("--config=classpath:io/bootique/job/fixture/scheduler_test_triggers.yml")
                .module(JobModule.class)
                .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addListener(listener))
                .createRuntime();
    }

    @After
    public void after() {
        getScheduler().getScheduledJobs().forEach(ScheduledJobFuture::cancelInterruptibly);
    }

    @Test
    public void testScheduler_StartWithNoJobs_Exception() {
        Scheduler scheduler = getScheduler();

        Exception e = null;
        try {
            scheduler.start(Collections.emptyList());
        } catch (Exception e1) {
            e = e1;
        }
        assertNotNull(e);
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().equals("No jobs specified"));
    }

    @Test
    public void testScheduler_StartWithUnknownJob_Exception() {
        Scheduler scheduler = getScheduler();

        Exception e = null;
        try {
            scheduler.start(Collections.singletonList("bogusjob123"));
        } catch (Exception e1) {
            e = e1;
        }
        assertNotNull(e);
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().equals("Unknown job: bogusjob123"));
    }

    @Test
    public void testScheduler_StartAfterPreviousCallToStartFailed() {
        Scheduler scheduler = getScheduler();

        try {
            scheduler.start(Collections.emptyList());
        } catch (Exception e) {
            // ignore
        }

        scheduler.start(Collections.singletonList("scheduledjob1"));
        assertTrue(scheduler.isStarted());
    }

    @Test
    public void testScheduler_Reschedule() {
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

        sleep(1000);

        assertEqualsApprox(85, 115, listener.getAverageRate());

        assertTrue(scheduledJob.cancel(false));
        assertFalse(scheduledJob.isScheduled());
        assertFalse(scheduledJob.getSchedule().isPresent());

        // allow for remaining jobs to complete gracefully
        // (Future.cancel() does not wait for actual completion)
        sleep(1000);

        listener.reset();

        // wait for a bit to ensure that no jobs are running in the background
        sleep(1000);

        assertEquals(0, listener.getAverageRate());

        assertTrue(scheduledJob.scheduleAtFixedRate(50, 0));
        assertTrue(scheduledJob.isScheduled());
        assertTrue(scheduledJob.getSchedule().isPresent());
        assertEquals("fixedRateMs: 50", scheduledJob.getSchedule().get().getDescription());

        sleep(1000);

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
