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

package io.bootique.job.runtime;

import io.bootique.job.Job;
import io.bootique.job.JobDecorator;
import io.bootique.job.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @since 3.0
 */
public class JobLogger implements JobDecorator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(JobLogger.class);

    @Override
    public JobResult run(Job delegate, Map<String, Object> params) {

        String name = delegate.getMetadata().getName();
        onJobStarted(name, params);

        try {
            JobResult result = delegate.run(params);
            return onJobFinished(name, result);
        } catch (Throwable th) {
            return onJobFinished(name, JobResult.failure(delegate.getMetadata(), th));
        }
    }

    private void onJobStarted(String name, Map<String, Object> params) {
        LOGGER.info(String.format("job '%s' started with params %s", name, params));
    }

    private JobResult onJobFinished(String name, JobResult result) {

        switch (result.getOutcome()) {
            case SUCCESS:
                LOGGER.info("job '{}' finished", name);
                return result;
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
                return result;
        }
    }
}
