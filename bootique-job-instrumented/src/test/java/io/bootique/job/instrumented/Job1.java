package io.bootique.job.instrumented;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;

import java.util.Map;

public class Job1 extends BaseJob {

    private boolean executed;

    public Job1() {
        super(JobMetadata.build("job1"));
    }

    @Override
    public JobResult run(Map<String, Object> params) {

        // stub
        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.executed = true;

        return JobResult.success(getMetadata());
    }

    public boolean isExecuted() {
        return executed;
    }
}
