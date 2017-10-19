package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class Job1 extends ExecutableAtMostOnceJob {

    public Job1() {
        this(0);
    }

    public Job1(long runningTime) {
        this(JobMetadata.build(Job1.class), runningTime, false);
    }

    public Job1(long runningTime, boolean shouldFail) {
        this(JobMetadata.build(Job1.class), runningTime, shouldFail);
    }

    public Job1(JobMetadata metadata) {
        this(metadata, 0, false);
    }

    public Job1(JobMetadata metadata, long runningTime) {
        super(metadata, runningTime, false);
    }

    public Job1(JobMetadata metadata, long runningTime, boolean shouldFail) {
        super(metadata, runningTime, shouldFail);
    }
}
