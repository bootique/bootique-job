package io.bootique.job.instrumented;

import io.bootique.BQRuntime;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.scheduler.ScheduledJobFuture;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.logback.LogbackModule;
import io.bootique.metrics.MetricsModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class InstrumentedJobMDCIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    private InstrumentedJobListener listener;
    private BQRuntime runtime;

    @Before
    public void before() {
        runtime = testFactory.app("-c", "classpath:io/bootique/job/instrumented/schedule.yml")
                .module(new LogbackModule())
                .module(new MetricsModule())
                .module(new JobModule())
                .module(new InstrumentedJobModule())
                .module(binder -> {
                    JobModule.extend(binder)
                            .addJob(ScheduledJob1.class)
                            .addJob(ScheduledJob2.class);

                }).createRuntime();
    }

    @After
    public void after() {
        getScheduler().getScheduledJobs().forEach(ScheduledJobFuture::cancelInterruptibly);
    }

    @Test
    public void testScheduleJob() throws InterruptedException {
        Scheduler scheduler = getScheduler();

        int jobCount = scheduler.start();
        assertEquals(2, jobCount);

        Collection<ScheduledJobFuture> scheduledJobs = scheduler.getScheduledJobs();
        assertEquals(2, scheduledJobs.size());

        Thread.sleep(1000);
    }

    private Scheduler getScheduler() {
        return runtime.getInstance(Scheduler.class);
    }
}
