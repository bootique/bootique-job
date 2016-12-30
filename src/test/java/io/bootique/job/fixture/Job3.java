package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class Job3 extends ExecutableJob {

    public Job3() {
        this(0);
    }

    public Job3(long runningTime) {
        super(JobMetadata.build(Job3.class), runningTime);
    }
}
