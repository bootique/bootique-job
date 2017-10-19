package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class ScheduledJob2 extends RepeatableJob {

    public ScheduledJob2() {
        super(JobMetadata.build(ScheduledJob2.class), 0);
    }
}
