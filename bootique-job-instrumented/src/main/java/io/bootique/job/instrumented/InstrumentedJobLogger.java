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

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobResult;
import io.bootique.job.runtime.JobLogger;

import java.util.Map;

/**
 * @since 3.0
 */
class InstrumentedJobLogger extends JobLogger {

    private final JobMDCManager mdcManager;
    private final JobMetricsManager metricsManager;

    InstrumentedJobLogger(JobMDCManager mdcManager, JobMetricsManager metricsManager) {
        this.mdcManager = mdcManager;
        this.metricsManager = metricsManager;
    }

    @Override
    public JobResult run(Job delegate, Map<String, Object> params) {
        JobMetadata metadata = delegate.getMetadata();
        JobMeter meter = onMeteredJobStarted(metadata, params);

        try {
            JobResult result = delegate.run(params);
            return onMeteredJobFinished(result, meter);
        } catch (Throwable th) {
            return onMeteredJobFinished(JobResult.failure(metadata, th), meter);
        }
    }

    protected JobMeter onMeteredJobStarted(JobMetadata metadata, Map<String, Object> params) {
        String label = metadata.isGroup() ? "group" : "job";
        String name = metadata.getName();

        mdcManager.onJobStarted();
        JobMeter meter = metricsManager.onJobStarted(name);
        LOGGER.info("{} '{}' started with params {}", label, name, params);
        return meter;
    }

    private JobResult onMeteredJobFinished(JobResult result, JobMeter meter) {
        long timeMs = meter.stop(result);
        logJobFinished(result, timeMs);
        mdcManager.onJobFinished();
        return result;
    }

    private void logJobFinished(JobResult result, long timeMs) {

        String label = result.getMetadata().isGroup() ? "group" : "job";
        String name = result.getMetadata().getName();

        switch (result.getOutcome()) {
            case SUCCESS:
                LOGGER.info("{} '{}' finished in {} ms", label, name, timeMs);
                return;

            default:
                String message = result.getMessage();
                if (message == null && result.getThrowable() != null) {
                    message = result.getThrowable().getMessage();
                }

                if (message == null) {
                    message = "";
                }

                if (result.getThrowable() != null) {
                    LOGGER.info("job exception", result.getThrowable());
                }

                LOGGER.warn("{} '{}' finished in {} ms: {} - {} ", label, name, timeMs, result.getOutcome(), message);
        }
    }
}
