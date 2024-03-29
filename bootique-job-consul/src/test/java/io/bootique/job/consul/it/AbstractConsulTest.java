package io.bootique.job.consul.it;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.job.JobsModule;
import io.bootique.job.Scheduler;
import io.bootique.job.consul.it.job.LockJob;
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
    static final GenericContainer consul = new GenericContainer("consul:1.15.4").withExposedPorts(8500);

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    protected Scheduler getSchedulerFromRuntime() {
        BQRuntime bqRuntime = testFactory
                .app("--config=classpath:io/bootique/job/consul/it/job-lock.yml")
                .autoLoadModules()
                .module(b -> BQCoreModule.extend(b).setProperty("bq.job-consul.consulPort", String.valueOf(consul.getMappedPort(8500))))
                .module(b -> JobsModule.extend(b).addJob(LockJob.class))
                .createRuntime();
        return bqRuntime.getInstance(Scheduler.class);
    }

}
