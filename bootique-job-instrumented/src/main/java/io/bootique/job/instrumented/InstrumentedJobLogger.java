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
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.execution.JobLogger;

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
        String name = delegate.getMetadata().getName();
        JobMeter meter = onMeteredJobStarted(name, params);

        try {
            JobResult result = delegate.run(params);
            return onMeteredJobFinished(name, result, meter);
        } catch (Throwable th) {
            return onMeteredJobFinished(name, JobResult.failure(delegate.getMetadata(), th), meter);
        }
    }

    protected JobMeter onMeteredJobStarted(String name, Map<String, Object> params) {
        mdcManager.onJobStarted();
        JobMeter meter = metricsManager.onJobStarted(name);
        LOGGER.info("job '{}' started with params {}", name, params);
        return meter;
    }

    private JobResult onMeteredJobFinished(String name, JobResult result, JobMeter meter) {
        long timeMs = meter.stop(result);
        logJobFinished(name, result, timeMs);
        mdcManager.onJobFinished();
        return result;
    }

    private void logJobFinished(String name, JobResult result, long timeMs) {

        switch (result.getOutcome()) {
            case SUCCESS:
                LOGGER.info("job '{}' finished in {} ms", name, timeMs);
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

                LOGGER.warn("job '{}' finished in {} ms: {} - {} ", name, timeMs, result.getOutcome(), message);
        }
    }
}
