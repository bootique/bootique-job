package io.bootique.job.instrumented;

import io.bootique.job.JobMetadata;

public class ScheduledJob1 extends RepeatableJob {

    public ScheduledJob1() {
        super(JobMetadata.build(ScheduledJob1.class), 0);
    }
}
