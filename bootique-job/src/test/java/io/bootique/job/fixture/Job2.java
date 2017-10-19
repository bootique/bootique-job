package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class Job2 extends ExecutableAtMostOnceJob {

    public Job2() {
        this(0);
    }

    public Job2(long runningTime) {
        this(JobMetadata.build(Job2.class), runningTime, false);
    }

    public Job2(long runningTime, boolean shouldFail) {
        this(JobMetadata.build(Job2.class), runningTime, shouldFail);
    }

    public Job2(JobMetadata metadata) {
        this(metadata, 0, false);
    }

    public Job2(JobMetadata metadata, long runningTime) {
        super(metadata, runningTime, false);
    }

    public Job2(JobMetadata metadata, long runningTime, boolean shouldFail) {
        super(metadata, runningTime, shouldFail);
    }
}
