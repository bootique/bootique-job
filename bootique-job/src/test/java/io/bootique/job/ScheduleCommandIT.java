package io.bootique.job;

import io.bootique.BQRuntime;
import io.bootique.job.fixture.ScheduledJob1;
import io.bootique.job.fixture.ScheduledJob2;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScheduleCommandIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testScheduleCommand_AllJobs() {
        BQRuntime runtime = testFactory.app()
                .args("--schedule", "-c", "classpath:io/bootique/job/fixture/scheduler_test_command.yml")
                .module(JobModule.class)
                .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addJob(ScheduledJob2.class))
                .createRuntime();

        Scheduler scheduler = runtime.getInstance(Scheduler.class);
        assertFalse(scheduler.isStarted());

        runtime.run();

        assertTrue(scheduler.isStarted());
        assertEquals(2, scheduler.getScheduledJobs().size());
    }

    @Test
    public void testScheduleCommand_SelectedJobs() {
        BQRuntime runtime = testFactory.app()
                .args("--schedule", "--job=scheduledjob1", "-c", "classpath:io/bootique/job/fixture/scheduler_test_triggers.yml")
                .module(JobModule.class)
                .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addJob(ScheduledJob2.class))
                .createRuntime();

        Scheduler scheduler = runtime.getInstance(Scheduler.class);

        runtime.run();

        assertTrue(scheduler.isStarted());
        assertEquals(1, scheduler.getScheduledJobs().size());
        assertEquals("scheduledjob1", scheduler.getScheduledJobs().iterator().next().getJobName());
    }
}
