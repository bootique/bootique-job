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
import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @since 3.0
 */
// calling the class JobLogger instead of InstrumentedJobLogDecorator for prettier log output
class JobLogger implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLogger.class);

    private final Job delegate;
    private final String name;
    private final JobMDCManager mdcManager;
    private final JobMetricsManager metricsManager;

    JobLogger(Job delegate, JobMDCManager mdcManager, JobMetricsManager metricsManager) {
        this.delegate = delegate;
        this.name = delegate.getMetadata().getName();
        this.mdcManager = mdcManager;
        this.metricsManager = metricsManager;
    }

    @Override
    public JobMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public JobResult run(Map<String, Object> params) {

        JobMeter meter = onJobStarted(params);

        try {
            JobResult result = delegate.run(params);
            return onJobFinished(result, meter);
        } catch (Throwable th) {
            // not expecting an exception because of the throwable job wrappers,
            // but still need to be prepared for any outcome...
            return onJobFinished(JobResult.failure(getMetadata(), th), meter);
        }
    }

    private JobMeter onJobStarted(Map<String, Object> params) {
        mdcManager.onJobStarted();
        JobMeter meter = metricsManager.onJobStarted(name);
        LOGGER.info("job '{}' started with params {}", name, params);
        return meter;
    }

    private JobResult onJobFinished(JobResult result, JobMeter meter) {
        long timeMs = meter.stop(result);
        logJobFinished(result, timeMs);
        mdcManager.onJobFinished();
        return result;
    }

    private void logJobFinished(JobResult result, long timeMs) {

        switch (result.getOutcome()) {
            case SUCCESS:
                LOGGER.info("job '{}' finished in {} ms", name, timeMs);
                return;
            case YIELDED:
                LOGGER.info("job '{}' yielded in {} ms", name, timeMs);
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
