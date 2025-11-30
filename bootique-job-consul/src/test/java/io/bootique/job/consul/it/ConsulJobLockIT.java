package io.bootique.job.consul.it;

import io.bootique.job.consul.it.job.LockJob;
import io.bootique.job.Scheduler;
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
    public void consulClusterJobLocking() throws InterruptedException {
        Scheduler scheduler_1 = getSchedulerFromRuntime();
        Scheduler scheduler_2 = getSchedulerFromRuntime();
        scheduler_1.newExecution().job(new LockJob()).params(callsCount).runNonBlocking();
        scheduler_2.newExecution().job(new LockJob()).params(callsCount).runNonBlocking();
        Thread.sleep(WAIT_TIME);
        assertEquals(1, callsCount.get(CALLS_COUNT));
    }

    @Test
    public void consulLocalJobLocking() throws InterruptedException {
        Scheduler scheduler = getSchedulerFromRuntime();
        scheduler.newExecution().job(new LockJob()).params(callsCount).runNonBlocking();
        scheduler.newExecution().job(new LockJob()).params(callsCount).runNonBlocking();
        Thread.sleep(WAIT_TIME);
        assertEquals(1, callsCount.get(CALLS_COUNT));
    }
}
