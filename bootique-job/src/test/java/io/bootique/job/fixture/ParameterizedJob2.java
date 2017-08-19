package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class ParameterizedJob2 extends ExecutableAtMostOnceJob {

    public ParameterizedJob2() {
        this(0);
    }

    public ParameterizedJob2(long runningTime) {
        super(JobMetadata.builder(ParameterizedJob2.class).longParam("longp").build(), runningTime);
    }
}
