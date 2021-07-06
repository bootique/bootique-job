package io.bootique.job.consul.it.job;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.SerialJob;
import io.bootique.job.consul.it.ConsulJobLockIT;
import io.bootique.job.runnable.JobResult;

import java.util.Map;

@SerialJob
public class LockJob extends BaseJob {

    private static final int DELAY = 1_000;

    public LockJob() {
        super(JobMetadata.build(LockJob.class));
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        Integer callsCount = (Integer) params.get(ConsulJobLockIT.CALLS_COUNT);
        params.put(ConsulJobLockIT.CALLS_COUNT, callsCount + 1);
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            return JobResult.failure(getMetadata());
        }
        return JobResult.success(getMetadata());
    }
}
