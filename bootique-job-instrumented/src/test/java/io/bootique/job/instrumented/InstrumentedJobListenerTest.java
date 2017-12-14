package io.bootique.job.instrumented;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import io.bootique.job.runnable.JobResult;
import io.bootique.metrics.mdc.SafeTransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InstrumentedJobListenerTest {

    private MetricRegistry metricRegistry;

    private TransactionIdMDC transactionIdMDC;
    private TransactionIdGenerator idGenerator;

    @Before
    public void before() {
        this.metricRegistry = new MetricRegistry();
        this.transactionIdMDC = new TransactionIdMDC();

        int cpus = Runtime.getRuntime().availableProcessors();
        if (cpus < 1) {
            cpus = 1;
        } else if (cpus > 4) {
            cpus = 4;
        }

        this.idGenerator = new SafeTransactionIdGenerator(cpus);
    }

    @Test
    public void testJobsInstrumentation_ActiveCount_SuccessAndFailureResults() {
        InstrumentedJobListener listener = new InstrumentedJobListener(metricRegistry, transactionIdMDC, idGenerator);

        FinishEventSource f1 = new FinishEventSource();
        listener.onJobStarted("j1", Collections.emptyMap(), f1);
        assertHasMetrics("j1", metricRegistry, 1, 0, 0, 0);

        FinishEventSource f2 = new FinishEventSource();
        listener.onJobStarted("j1", Collections.emptyMap(), f2);
        assertHasMetrics("j1", metricRegistry, 2, 0, 0, 0);

        f1.finish(JobResult.success(null));
        assertHasMetrics("j1", metricRegistry, 1, 1, 1, 0);

        f2.finish(JobResult.failure(null));
        assertHasMetrics("j1", metricRegistry, 0, 2, 1, 1);
    }

    @Test
    public void testJobsInstrumentation_UnknownResult() {
        InstrumentedJobListener listener = new InstrumentedJobListener(metricRegistry, transactionIdMDC, idGenerator);

        FinishEventSource f1 = new FinishEventSource();
        listener.onJobStarted("j1", Collections.emptyMap(), f1);
        assertHasMetrics("j1", metricRegistry, 1, 0, 0, 0);

        f1.finish(JobResult.unknown(null));
        assertHasMetrics("j1", metricRegistry, 0, 1, 0, 0);
    }

    @Test
    public void testJobsInstrumentation_SuccessResult() {
        FinishEventSource finishEventSource = new FinishEventSource();

        InstrumentedJobListener listener = new InstrumentedJobListener(metricRegistry, transactionIdMDC, idGenerator);
        listener.onJobStarted("j1", Collections.emptyMap(), finishEventSource);
        finishEventSource.finish(JobResult.unknown(null));

        assertHasMetrics("j1", metricRegistry, 0, 1, 0, 0);
    }

    private void assertHasMetrics(String jobName,
                                  MetricRegistry metricRegistry,
                                  int active,
                                  int completed,
                                  int success,
                                  int failure) {
        Counter activeCounter = metricRegistry.getCounters().get(InstrumentedJobListener.getActiveCounterName(jobName));
        assertNotNull(activeCounter);
        assertEquals(active, activeCounter.getCount());

        Counter completedCounter = metricRegistry.getCounters().get(InstrumentedJobListener.getCompletedCounterName(jobName));
        assertNotNull(completedCounter);
        assertEquals(completed, completedCounter.getCount());

        Counter successCounter = metricRegistry.getCounters().get(InstrumentedJobListener.getSuccessCounterName(jobName));
        assertNotNull(successCounter);
        assertEquals(success, successCounter.getCount());

        Counter failureCounter = metricRegistry.getCounters().get(InstrumentedJobListener.getFailureCounterName(jobName));
        assertNotNull(failureCounter);
        assertEquals(failure, failureCounter.getCount());
    }


    static class FinishEventSource implements Consumer<Consumer<JobResult>> {
        private Collection<Consumer<JobResult>> finishEventListeners = new ArrayList<>();
        private boolean finished;

        @Override
        public void accept(Consumer<JobResult> listener) {
            finishEventListeners.add(listener);
        }

        void finish(JobResult result) {
            if (finished) {
                throw new IllegalStateException("Already finished");
            }
            finishEventListeners.forEach(l -> l.accept(result));
            finished = true;
        }
    }
}
