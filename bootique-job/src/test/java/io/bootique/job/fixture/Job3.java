package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class Job3 extends ExecutableAtMostOnceJob {

    public Job3() {
        this(0);
    }

    public Job3(long runningTime) {
        this(JobMetadata.build(Job3.class), runningTime, false);
    }

    public Job3(long runningTime, boolean shouldFail) {
        this(JobMetadata.build(Job3.class), runningTime, shouldFail);
    }

    public Job3(JobMetadata metadata) {
        this(metadata, 0, false);
    }

    public Job3(JobMetadata metadata, long runningTime) {
        super(metadata, runningTime, false);
    }

    public Job3(JobMetadata metadata, long runningTime, boolean shouldFail) {
        super(metadata, runningTime, shouldFail);
    }
}
