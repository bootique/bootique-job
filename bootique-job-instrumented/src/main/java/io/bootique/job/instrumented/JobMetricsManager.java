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

import com.codahale.metrics.MetricRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects job execution metrics and posts them to the metrics registry.
 *
 * @since 3.0
 */
public class JobMetricsManager {

    private final MetricRegistry metricRegistry;
    private final Map<String, JobMetrics> metrics;

    public JobMetricsManager(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.metrics = new ConcurrentHashMap<>();
    }

    public JobMeter onJobStarted(String jobName) {
        JobMeter meter = new JobMeter(getOrCreateMetrics(jobName));
        meter.start();
        return meter;
    }

    private JobMetrics getOrCreateMetrics(String jobName) {
        return metrics.computeIfAbsent(jobName, n -> new JobMetrics(metricRegistry, jobName));
    }
}
