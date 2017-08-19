package io.bootique.job.fixture;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;

import java.util.Map;

public abstract class RepeatableJob extends BaseJob {

    private final long runningTime;

    public RepeatableJob(JobMetadata metadata, long runningTime) {
        super(metadata);
        this.runningTime = runningTime;
    }

    @Override
    public synchronized JobResult run(Map<String, Object> params) {
        busyWait(runningTime);
        return JobResult.success(getMetadata());
    }

    private void busyWait(long time) {
        long i = 0;
        while (i++ < time)
            ;
    }
}
