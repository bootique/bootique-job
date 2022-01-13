package io.bootique.job.consul.it;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.job.consul.ConsulJobModule;
import io.bootique.job.consul.it.job.LockJob;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@BQTest
public abstract class AbstractConsulTest {

    @Container
    static final GenericContainer consul = new GenericContainer("consul:latest").withExposedPorts(8500);

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    protected Scheduler getSchedulerFromRuntime(String yamlConfigPath) {
        BQRuntime bqRuntime = testFactory
                .app(yamlConfigPath)
                .module(b -> BQCoreModule.extend(b).setProperty("bq.job-consul.consulPort", String.valueOf(consul.getMappedPort(8500))))
                .override(JobModule.class).with(ConsulJobModule.class)
                .module(new JobModule())
                .module(b -> JobModule.extend(b).addJob(LockJob.class))
                .createRuntime();
        return bqRuntime.getInstance(Scheduler.class);
    }

}
