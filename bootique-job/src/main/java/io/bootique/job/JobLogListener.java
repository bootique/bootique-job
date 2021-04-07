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

package io.bootique.job;

import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @since 0.25
 */
public class JobLogListener implements JobListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLogListener.class);

    @Override
    public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
        LOGGER.info(String.format("job '%s' started with params %s", jobName, parameters));
        finishEventSource.accept(result -> {

            if (result.isSuccess()) {
                LOGGER.info("job '{}' finished", jobName);
            } else {
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

                LOGGER.warn("job '{}' finished: {} - {} ", jobName, result.getOutcome(), message);
            }
        });
    }
}
