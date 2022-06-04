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
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A collection of unrelated jobs within a group that can be executed in parallel.
 *
 * @since 3.0
 */
public abstract class JobGroupStep implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobGroupStep.class);

    protected final Scheduler scheduler;

    protected JobGroupStep(Scheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler);
    }

    protected void logResult(JobResult result) {
        if (result.isSuccess()) {
            LOGGER.info("group member '{}' finished", result.getMetadata().getName());
        } else {

            LOGGER.info("group member '{}' finished: {} - {}",
                    result.getMetadata().getName(),
                    result.getOutcome(),
                    result.getMessage());

            if (result.getThrowable() != null) {
                LOGGER.error("group member error", result.getThrowable());
            }
        }
    }
}
