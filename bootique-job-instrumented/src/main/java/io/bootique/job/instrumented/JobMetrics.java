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
import io.bootique.metrics.MetricNaming;

/**
 * @since 3.0
 */
class JobMetrics {

    private static final MetricNaming NAMING = MetricNaming.forModule(JobInstrumentedModule.class);

    private Counter activeCounter, completedCounter, successCounter, failureCounter;
    private Timer timer;

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
