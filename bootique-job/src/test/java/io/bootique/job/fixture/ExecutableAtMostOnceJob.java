package io.bootique.job.fixture;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ExecutableAtMostOnceJob extends BaseJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutableAtMostOnceJob.class);

    private final long runningTime;
    private volatile boolean executed;
    private volatile Map<String, Object> params;
    private volatile long startedAt;
    private volatile long finishedAt;

    private final ReentrantLock executionLock;

    public ExecutableAtMostOnceJob(JobMetadata metadata, long runningTime) {
        super(metadata);
        this.runningTime = runningTime;
        this.executionLock = new ReentrantLock();
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        try {
            boolean acquired = executionLock.tryLock();
            if (!acquired) {
                LOGGER.info("Failed to acquire lock; will wait until it becomes free...");
                try {
                    executionLock.lockInterruptibly();
                    LOGGER.info("Successfully acquired lock; will execute...");
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting for lock", e);
                }
            }

            return doExecute(params);
        } catch (Exception e) {
            LOGGER.error("Execution failed with exception; releasing lock...");
            throw e;
        } finally {
            executionLock.unlock();
        }
    }

    private JobResult doExecute(Map<String, Object> params) {
        if (executed) {
            throw new RuntimeException("Already executed: " + getMetadata().getName());
        }
        this.params = params;
        startedAt = System.nanoTime();
        busyWait(runningTime);
        finishedAt = System.nanoTime();
        executed = true;
        return JobResult.success(getMetadata());
    }

    private void busyWait(long time) {
        long i = 0;
        while (i++ < time)
            ;
    }

    public boolean isExecuted() {
        return finishedAt > 0;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
