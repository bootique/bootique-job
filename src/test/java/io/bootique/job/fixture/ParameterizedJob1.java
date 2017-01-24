package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class ParameterizedJob1 extends ExecutableJob {

    public ParameterizedJob1() {
        this(0);
    }

    public ParameterizedJob1(long runningTime) {
        super(JobMetadata.builder(ParameterizedJob1.class).longParam("longp", 777L).build(), runningTime);
    }
}
