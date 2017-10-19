package io.bootique.job;

import io.bootique.BQRuntime;
import io.bootique.job.fixture.ScheduledJob1;
import io.bootique.job.fixture.ScheduledJob2;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.scheduler.ScheduledJobFuture;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import static io.bootique.job.Utils.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScheduleCommandIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testScheduleCommand_AllJobs() {
        BQRuntime runtime = testFactory.app()
                .args("--config=classpath:io/bootique/job/fixture/scheduler_test_command.yml", "--schedule")
                .module(JobModule.class)
                .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addJob(ScheduledJob2.class))
                .createRuntime();

        Scheduler scheduler = runtime.getInstance(Scheduler.class);

        try {
            Thread t = new Thread(runtime::run);
            t.start();

            sleep(1000);

            assertTrue(scheduler.isStarted());
            assertEquals(2, scheduler.getScheduledJobs().size());

        } finally {
            scheduler.getScheduledJobs().forEach(ScheduledJobFuture::cancelInterruptibly);
        }
    }

    @Test
    public void testScheduleCommand_SelectedJobs() {
        BQRuntime runtime = testFactory.app()
                .args("--config=classpath:io/bootique/job/fixture/scheduler_test_triggers.yml", "--schedule", "--job=scheduledjob1")
                .module(JobModule.class)
                .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addJob(ScheduledJob2.class))
                .createRuntime();

        Scheduler scheduler = runtime.getInstance(Scheduler.class);

        try {
            Thread t = new Thread(runtime::run);
            t.start();

            sleep(1000);

            assertTrue(scheduler.isStarted());
            assertEquals(1, scheduler.getScheduledJobs().size());
            assertEquals("scheduledjob1", scheduler.getScheduledJobs().iterator().next().getJobName());

        } finally {
            scheduler.getScheduledJobs().forEach(ScheduledJobFuture::cancelInterruptibly);
        }
    }
}
