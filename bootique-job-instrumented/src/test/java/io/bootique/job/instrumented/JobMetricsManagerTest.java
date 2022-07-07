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
import io.bootique.job.JobResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class JobMetricsManagerTest {

    private MetricRegistry metricRegistry;

    @BeforeEach
    public void before() {
        this.metricRegistry = new MetricRegistry();
    }

    @Test
    public void testJobsInstrumentation_ActiveCount_SuccessAndFailureResults() {
        JobMetricsManager manager = new JobMetricsManager(metricRegistry);

        JobMeter m1 = manager.onJobStarted("j1");
        assertHasMetrics("j1", metricRegistry, 1, 0, 0, 0);

        JobMeter m2 = manager.onJobStarted("j1");
        assertHasMetrics("j1", metricRegistry, 2, 0, 0, 0);

        m1.stop(JobResult.success(null));
        assertHasMetrics("j1", metricRegistry, 1, 1, 1, 0);

        m2.stop(JobResult.failure(null));
        assertHasMetrics("j1", metricRegistry, 0, 2, 1, 1);
    }

    @Test
    public void testJobsInstrumentation_UnknownResult() {
        JobMetricsManager manager = new JobMetricsManager(metricRegistry);

        JobMeter m1 = manager.onJobStarted("j1");
        assertHasMetrics("j1", metricRegistry, 1, 0, 0, 0);

        m1.stop(JobResult.unknown(null));
        assertHasMetrics("j1", metricRegistry, 0, 1, 0, 0);
    }

    @Test
    public void testJobsInstrumentation_SuccessResult() {

        JobMetricsManager manager = new JobMetricsManager(metricRegistry);
        JobMeter m1 =  manager.onJobStarted("j1");
        m1.stop(JobResult.unknown(null));

        assertHasMetrics("j1", metricRegistry, 0, 1, 0, 0);
    }

    private void assertHasMetrics(String jobName,
                                  MetricRegistry metricRegistry,
                                  int active,
                                  int completed,
                                  int success,
                                  int failure) {
        Counter activeCounter = metricRegistry.getCounters().get(JobMetrics.activeCounterMetric(jobName));
        assertNotNull(activeCounter);
        assertEquals(active, activeCounter.getCount());

        Counter completedCounter = metricRegistry.getCounters().get(JobMetrics.completedCounterMetric(jobName));
        assertNotNull(completedCounter);
        assertEquals(completed, completedCounter.getCount());

        Counter successCounter = metricRegistry.getCounters().get(JobMetrics.successCounterMetric(jobName));
        assertNotNull(successCounter);
        assertEquals(success, successCounter.getCount());

        Counter failureCounter = metricRegistry.getCounters().get(JobMetrics.failureCounterMetric(jobName));
        assertNotNull(failureCounter);
        assertEquals(failure, failureCounter.getCount());
    }
}
