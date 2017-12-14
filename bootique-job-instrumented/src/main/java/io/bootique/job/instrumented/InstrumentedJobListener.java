package io.bootique.job.instrumented;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import io.bootique.job.JobListener;
import io.bootique.job.runnable.JobResult;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @since 0.14
 */
public class InstrumentedJobListener implements JobListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentedJobListener.class);

    private MetricRegistry metricRegistry;
    private Map<String, JobMetrics> metrics;
    private ReentrantLock lock;

    private TransactionIdMDC transactionIdMDC;
    private TransactionIdGenerator idGenerator;

    @Inject
    public InstrumentedJobListener(MetricRegistry metricRegistry,
                                   TransactionIdMDC transactionIdMDC,
                                   TransactionIdGenerator idGenerator) {
        this.metricRegistry = metricRegistry;
        this.metrics = new HashMap<>();
        this.lock = new ReentrantLock();
        this.transactionIdMDC = transactionIdMDC;
        this.idGenerator = idGenerator;
    }

    @Override
    public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
        String id = idGenerator.nextId();
        transactionIdMDC.reset(id);

        JobMetrics metric = getOrCreateMetrics(jobName);

        metric.getActiveCounter().inc();
        Timer.Context requestTimerContext = metric.getTimer().time();

        LOGGER.info("started job: '{}'", jobName);

        finishEventSource.accept(result -> {
            metric.getActiveCounter().dec();
            metric.getCompletedCounter().inc();
            // Timer.Context#stop also updates aggregate running time of all instances of <jobName>
            long timeNanos = requestTimerContext.stop();
            LOGGER.info("finished job '{}' in {} ms", jobName, timeNanos / 1000000);

            switch (result.getOutcome()) {
                case SUCCESS: {
                    metric.getSuccessCounter().inc();
                    break;
                }
                case FAILURE: {
                    metric.getFailureCounter().inc();
                    break;
                }
                // do not track other results
            }
        });
    }

    // using explicit synchronization as the metric registry does not prevent data race
    private JobMetrics getOrCreateMetrics(String jobName) {
        JobMetrics metric = metrics.get(jobName);
        if (metric == null) {
            lock.lock();
            metric = metrics.get(jobName);
            if (metric == null) {
                try {
                    metric = new JobMetrics(metricRegistry, jobName);
                    metrics.put(jobName, metric);
                } finally {
                    lock.unlock();
                }
            }
        }
        return metric;
    }

    static String getActiveCounterName(String jobName) {
        return getCounterName(jobName, "active");
    }

    static String getCompletedCounterName(String jobName) {
        return getCounterName(jobName, "completed");
    }

    static String getSuccessCounterName(String jobName) {
        return getCounterName(jobName, "success");
    }

    static String getFailureCounterName(String jobName) {
        return getCounterName(jobName, "failure");
    }

    /**
     * @param aspect What's being counted
     */
    private static String getCounterName(String jobName, String aspect) {
        return MetricRegistry.name(InstrumentedJobListener.class, jobName + "-" + aspect + "-counter");
    }

    static String getTimerName(String jobName) {
        return MetricRegistry.name(InstrumentedJobListener.class, jobName + "-timer");
    }

    private static class JobMetrics {
        private Counter activeCounter, completedCounter, successCounter, failureCounter;
        private Timer timer;

        JobMetrics(MetricRegistry metricRegistry, String jobName) {
            this.activeCounter = metricRegistry.counter(getActiveCounterName(jobName));
            this.completedCounter = metricRegistry.counter(getCompletedCounterName(jobName));
            this.successCounter = metricRegistry.counter(getSuccessCounterName(jobName));
            this.failureCounter = metricRegistry.counter(getFailureCounterName(jobName));
            this.timer = metricRegistry.timer(getTimerName(jobName));
        }

        Counter getActiveCounter() {
            return activeCounter;
        }

        Counter getCompletedCounter() {
            return completedCounter;
        }

        Counter getSuccessCounter() {
            return successCounter;
        }

        Counter getFailureCounter() {
            return failureCounter;
        }

        Timer getTimer() {
            return timer;
        }
    }
}
