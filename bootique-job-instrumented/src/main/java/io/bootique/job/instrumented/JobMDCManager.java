package io.bootique.job.instrumented;

import io.bootique.job.JobListener;
import io.bootique.job.runnable.JobResult;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @since 0.25
 */
public class JobMDCManager implements JobListener {
    private TransactionIdGenerator idGenerator;
    private TransactionIdMDC transactionIdMDC;

    public JobMDCManager(TransactionIdGenerator idGenerator, TransactionIdMDC transactionIdMDC) {
        this.idGenerator = idGenerator;
        this.transactionIdMDC = transactionIdMDC;
    }

    @Override
    public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
        String id = idGenerator.nextId();
        transactionIdMDC.reset(id);
    }
}