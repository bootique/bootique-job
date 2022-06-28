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

package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @since 3.0
 */
// calling the class JobLogger instead of JobLogDecorator for prettier log output
class JobLogger implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLogger.class);

    private final Job delegate;
    private final String name;

    JobLogger(Job delegate) {
        this.delegate = delegate;
        this.name = delegate.getMetadata().getName();
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
        LOGGER.info(String.format("job '%s' started with params %s", name, params));
    }

    private void onJobFinished(JobResult result) {

        switch (result.getOutcome()) {
            case SUCCESS:
                LOGGER.info("job '{}' finished", name);
                break;
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

                LOGGER.warn("job '{}' finished: {} - {} ", name, result.getOutcome(), message);
        }
    }
}
