package io.bootique.job.job;

import io.bootique.job.JobMetadata;

public class Job2 extends ExecutableJob {

    public Job2(long runningTime) {
        super(JobMetadata.build(Job2.class), runningTime);
    }
}
