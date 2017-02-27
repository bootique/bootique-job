package io.bootique.job.fixture;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;

import java.util.Map;

public abstract class ExecutableJob extends BaseJob {

    private final long runningTime;
    private volatile boolean executed;
    private volatile Map<String, Object> params;
    private volatile long startedAt;
    private volatile long finishedAt;

    public ExecutableJob(JobMetadata metadata, long runningTime) {
        super(metadata);
        this.runningTime = runningTime;
    }

    @Override
    public JobResult run(Map<String, Object> params) {
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
