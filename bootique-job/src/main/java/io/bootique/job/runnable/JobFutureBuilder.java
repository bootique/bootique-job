package io.bootique.job.runnable;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

/**
 * @since 0.24
 */
public class JobFutureBuilder {

    private String job;
    private RunnableJob runnable;
    private ScheduledFuture<?> future;
    private Supplier<JobResult> resultSupplier;

    public JobFutureBuilder(String job) {
        this.job = Objects.requireNonNull(job);
    }

    public JobFutureBuilder runnable(RunnableJob runnable) {
        this.runnable = Objects.requireNonNull(runnable);
        return this;
    }

    public JobFutureBuilder future(ScheduledFuture<?> future) {
        this.future = future;
        return this;
    }

    public JobFutureBuilder resultSupplier(Supplier<JobResult> resultSupplier) {
        this.resultSupplier = resultSupplier;
        return this;
    }

    public JobFuture build() {
        Objects.requireNonNull(future);
        Objects.requireNonNull(resultSupplier);
        return new DefaultJobFuture(job, runnable, future, resultSupplier);
    }
}
