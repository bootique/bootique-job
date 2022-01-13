package io.bootique.job.consul.it;

import io.bootique.job.consul.it.job.LockJob;
import io.bootique.job.scheduler.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConsulJobLockIT extends AbstractConsulTest {

    public static final String CALLS_COUNT = "count";
    private static final int WAIT_TIME = 3_000;

    private Map<String, Object> callsCount;

    @BeforeEach
    public void before() {
        callsCount = new ConcurrentHashMap<>();
        callsCount.put(CALLS_COUNT, 0);
    }

    @Test
    public void testConsulClusterJobLocking() throws InterruptedException {
        Scheduler scheduler_1 = getSchedulerFromRuntime();
        Scheduler scheduler_2 = getSchedulerFromRuntime();
        scheduler_1.runOnce(new LockJob(), callsCount);
        scheduler_2.runOnce(new LockJob(), callsCount);
        Thread.sleep(WAIT_TIME);
        assertEquals(1, callsCount.get(CALLS_COUNT));
    }

    @Test
    public void testConsulLocalJobLocking() throws InterruptedException {
        Scheduler scheduler = getSchedulerFromRuntime();
        scheduler.runOnce(new LockJob(), callsCount);
        scheduler.runOnce(new LockJob(), callsCount);
        Thread.sleep(WAIT_TIME);
        assertEquals(1, callsCount.get(CALLS_COUNT));
    }
}
