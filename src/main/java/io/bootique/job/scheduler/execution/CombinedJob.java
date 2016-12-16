package io.bootique.job.scheduler.execution;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

class CombinedJob extends BaseJob {

    public static Builder builder(JobMetadata metadata) {
        return new Builder(metadata);
    }

    private List<Supplier<JobFuture>> jobs;

    public CombinedJob(JobMetadata metadata, List<Supplier<JobFuture>> jobs) {
        super(metadata);
        this.jobs = jobs;
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        for (Supplier<JobFuture> job : jobs) {
            // run sequentially
            JobResult result = job.get().get();
            if (result.getOutcome() == JobOutcome.FAILURE) {
                String message = "Failed to execute job: " + result.getMetadata().getName();
                if (result.getMessage() != null) {
                    message += ". Reason: " + result.getMessage();
                }
                return JobResult.failure(getMetadata(), message);
            }
        }

        return JobResult.success(getMetadata());
    }

    public static class Builder {

        private JobMetadata metadata;
        private List<Supplier<JobFuture>> jobs;

        private Builder(JobMetadata metadata) {
            this.metadata = metadata;
            this.jobs = new ArrayList<>();
        }

        public Builder thenRun(Supplier<JobFuture> job) {
            jobs.add(job);
            return this;
        }

        public CombinedJob build() {
            if (jobs.isEmpty()) {
                throw new IllegalStateException("No jobs");
            }
            return new CombinedJob(metadata, jobs);
        }
    }
}
