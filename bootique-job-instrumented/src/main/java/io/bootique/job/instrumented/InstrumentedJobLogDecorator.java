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
class InstrumentedJobLogDecorator implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentedJobLogDecorator.class);

    private final Job delegate;
    private final String name;
    private final JobMDCManager mdcManager;

    InstrumentedJobLogDecorator(Job delegate, JobMDCManager mdcManager) {
        this.delegate = delegate;
        this.name = delegate.getMetadata().getName();
        this.mdcManager = mdcManager;
    }

    @Override
    public JobMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        onJobStarted(params);

        JobResult result = delegate.run(params);

        onJobFinished(result);
        return result;
    }

    private void onJobStarted(Map<String, Object> params) {

        mdcManager.onJobStarted();
        LOGGER.info(String.format("job '%s' started with params %s", name, params));
    }

    private void onJobFinished(JobResult result) {

        if (result.isSuccess()) {
            LOGGER.info("job '{}' finished", name);
            return;
        }

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

        LOGGER.warn("job '{}' finished: {} - {} ", name, result.getOutcome(), message);

        mdcManager.onJobFinished();
    }
}
