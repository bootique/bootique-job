package io.bootique.job.consul.it;

import io.bootique.job.consul.it.job.LockJob;
import io.bootique.job.scheduler.Scheduler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsulJobLockIT extends AbstractConsulTest {

    private static final String CONFIG_PATH = "--config=classpath:io/bootique/job/consul/it/job-lock.yml";
    public static final String CALLS_COUNT = "count";
    private static final int WAIT_TIME = 3_000;

    private Map<String, Object> callsCount;

    @Before
    public void before() {
        callsCount = new ConcurrentHashMap<>();
        callsCount.put(CALLS_COUNT, 0);
    }

    @Test
    // Test on 2 clients and 1 Consul agent (Cluster lock)
    public void testConsulClusterJobLocking() throws InterruptedException {
        Scheduler scheduler_1 = getSchedulerFromRuntime(CONFIG_PATH);
        Scheduler scheduler_2 = getSchedulerFromRuntime(CONFIG_PATH);
        scheduler_1.runOnce(new LockJob(), callsCount);
        scheduler_2.runOnce(new LockJob(), callsCount);
        Thread.sleep(WAIT_TIME); // Wait all jobs
        Assert.assertEquals(1, callsCount.get(CALLS_COUNT));
    }

    @Test
    // Test on 1 client and 1 Consul agent (Local lock)
    public void testConsulLocalJobLocking() throws InterruptedException {
        Scheduler scheduler = getSchedulerFromRuntime(CONFIG_PATH);
        scheduler.runOnce(new LockJob(), callsCount);
        scheduler.runOnce(new LockJob(), callsCount);
        Thread.sleep(WAIT_TIME);
        // Expected 1 job execution, but actually we have 2 executions
        Assert.assertEquals(1, callsCount.get(CALLS_COUNT));
    }
}
