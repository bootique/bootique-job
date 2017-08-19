package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class Job2 extends ExecutableAtMostOnceJob {

    public Job2() {
        this(0);
    }

    public Job2(long runningTime) {
        super(JobMetadata.build(Job2.class), runningTime);
    }
}
