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


import io.bootique.job.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A collection of jobs within an execution graph that are independent of each other and can be run in parallel.
 *
 * @since 3.0
 */
public abstract class GraphJobStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphJobStep.class);

    public abstract JobResult run(Map<String, Object> params);

    protected void logResult(String jobName, JobResult result) {

        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        if (result.isSuccess()) {
            LOGGER.debug("graph member '{}' finished", jobName);
        } else {

            LOGGER.debug("graph member '{}' finished: {} - {}",
                    jobName,
                    result.getOutcome(),
                    result.getMessage());

            if (result.getThrowable() != null) {
                LOGGER.debug("graph member error", result.getThrowable());
            }
        }
    }
}
