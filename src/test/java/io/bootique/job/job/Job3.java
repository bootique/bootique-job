package io.bootique.job.job;

import io.bootique.job.JobMetadata;

public class Job3 extends ExecutableJob {

    public Job3(long runningTime) {
        super(JobMetadata.build(Job3.class), runningTime);
    }
}
