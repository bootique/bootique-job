package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;
import io.bootique.job.SerialJob;

@SerialJob
public class SerialJob1 extends Job1 {
    private static final long DEFAULT_RUNNING_TIME = 10_000_000_000L; // not a real time, but approx. CPU cycles

    public SerialJob1() {
        this(DEFAULT_RUNNING_TIME);
    }

    public SerialJob1(long runningTime) {
        super(JobMetadata.build(SerialJob1.class), runningTime);
    }
}
