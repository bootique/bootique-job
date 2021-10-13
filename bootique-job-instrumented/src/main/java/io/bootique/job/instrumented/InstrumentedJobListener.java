/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.job.instrumented;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.bootique.job.JobListener;
import io.bootique.job.runnable.JobResult;
import io.bootique.metrics.MetricNaming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A job listener that collects job execution metrics and posts them to the metrics registry.
 */
public class InstrumentedJobListener implements JobListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentedJobListener.class);

    private static final MetricNaming NAMING = MetricNaming.forModule(JobInstrumentedModule.class);

    private MetricRegistry metricRegistry;
    private Map<String, JobMetrics> metrics;

    public InstrumentedJobListener(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.metrics = new ConcurrentHashMap<>();
    }

    static String activeCounterMetric(String jobName) {
        return NAMING.name(jobName, "Active");
    }

    static String completedCounterMetric(String jobName) {
        return NAMING.name(jobName, "Completed");
    }

    static String successCounterMetric(String jobName) {
        return NAMING.name(jobName, "Success");
    }

    static String failureCounterMetric(String jobName) {
        return NAMING.name(jobName, "Failure");
    }

    static String timerMetric(String jobName) {
        return NAMING.name(jobName, "Time");
    }

    @Override
    public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {

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

    private JobMetrics getOrCreateMetrics(String jobName) {
        return metrics.computeIfAbsent(jobName, n -> new JobMetrics(metricRegistry, jobName));
    }

    private static class JobMetrics {
        private Counter activeCounter, completedCounter, successCounter, failureCounter;
        private Timer timer;

        JobMetrics(MetricRegistry metricRegistry, String jobName) {
            this.activeCounter = metricRegistry.counter(activeCounterMetric(jobName));
            this.completedCounter = metricRegistry.counter(completedCounterMetric(jobName));
            this.successCounter = metricRegistry.counter(successCounterMetric(jobName));
            this.failureCounter = metricRegistry.counter(failureCounterMetric(jobName));
            this.timer = metricRegistry.timer(timerMetric(jobName));
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
