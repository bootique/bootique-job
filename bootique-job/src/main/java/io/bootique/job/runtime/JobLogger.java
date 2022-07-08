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
import io.bootique.job.JobMetadata;
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

        JobMetadata metadata = delegate.getMetadata();
        onJobStarted(metadata, params);

        try {
            JobResult result = delegate.run(params);
            return onJobFinished(result);
        } catch (Throwable th) {
            return onJobFinished(JobResult.failure(metadata, th));
        }
    }

    private void onJobStarted(JobMetadata metadata, Map<String, Object> params) {
        String label = metadata.isGroup() ? "group" : "job";
        LOGGER.info("{} '{}' started with params {}", label, metadata.getName(), params);
    }

    private JobResult onJobFinished(JobResult result) {
        String label = result.getMetadata().isGroup() ? "group" : "job";
        String name = result.getMetadata().getName();

        switch (result.getOutcome()) {
            case SUCCESS:
                LOGGER.info("{} '{}' finished", label, name);
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

                LOGGER.warn("{} '{}' finished: {} - {} ", label, name, result.getOutcome(), message);
                return result;
        }
    }
}
