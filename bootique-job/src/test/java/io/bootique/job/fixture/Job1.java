package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class Job1 extends ExecutableAtMostOnceJob {

    public Job1() {
        this(0);
    }

    public Job1(long runningTime) {
        this(JobMetadata.build(Job1.class), runningTime);
    }

    public Job1(JobMetadata metadata) {
        this(metadata, 0);
    }

    public Job1(JobMetadata metadata, long runningTime) {
        super(metadata, runningTime);
    }
}
