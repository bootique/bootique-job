package io.bootique.job.job;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;

import java.util.Map;

public abstract class ExecutableJob extends BaseJob {

    private final long runningTime;
    private volatile long startedAt;
    private volatile long finishedAt;

    public ExecutableJob(JobMetadata metadata, long runningTime) {
        super(metadata);
        this.runningTime = runningTime;
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        startedAt = System.nanoTime();
        busyWait(runningTime);
        finishedAt = System.nanoTime();
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
}
