package io.bootique.job.job;

import io.bootique.job.JobMetadata;

public class Job1 extends ExecutableJob {

    public Job1(long runningTime) {
        super(JobMetadata.build(Job1.class), runningTime);
    }
}
