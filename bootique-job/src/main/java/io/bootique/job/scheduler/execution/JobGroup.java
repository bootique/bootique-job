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

import io.bootique.job.BaseJob;
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since 3.0
 */
class JobGroup extends BaseJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobGroup.class);

    private final Scheduler scheduler;
    private final List<Set<Job>> executionPlan;

    JobGroup(JobMetadata groupMetadata, List<Set<Job>> executionPlan, Scheduler scheduler) {
        super(groupMetadata);

        this.scheduler = scheduler;
        this.executionPlan = executionPlan;
    }

    @Override
    public JobResult run(Map<String, Object> params) {

        for (Set<Job> parallelBatch : executionPlan) {
            runBatchInParallel(parallelBatch, params);
        }

        return JobResult.success(getMetadata());
    }

    void runBatchInParallel(Set<Job> batch, Map<String, Object> params) {

        if (batch.isEmpty()) {
            return;
        }

        Set<JobResult> failures = new HashSet<>();

        batch.stream()
                .map(j -> scheduler.runOnce(j, params))
                .map(JobFuture::get)
                .forEach(r -> processJobResult(r, failures));

        processBatchFailures(failures);
    }

    private void processJobResult(JobResult result, Set<JobResult> failures) {
        if (result.isSuccess()) {
            LOGGER.info("Finished group child job '{}', result: {}",
                    result.getMetadata().getName(),
                    result.getOutcome());
        } else if (result.getThrowable() == null) {
            failures.add(result);
            LOGGER.info("Finished group child job '{}', result: {}, message: {}",
                    result.getMetadata().getName(),
                    result.getOutcome(),
                    result.getMessage());
        } else {
            failures.add(result);
            // have to use String.format instead of LOGGER substitutions because of the throwable parameter
            LOGGER.error(String.format("Finished group child job '%s', result: %s, message: %s",
                            result.getMetadata().getName(),
                            result.getOutcome(),
                            result.getMessage()),
                    result.getThrowable());
        }
    }

    private void processBatchFailures(Set<JobResult> failures) {
        if (!failures.isEmpty()) {

            // TODO: report all failure, not just the first one
            JobResult f1 = failures.iterator().next();

            String message = "Failed to execute at least one job: " + f1.getMetadata().getName();
            if (f1.getMessage() != null) {
                message += ". Reason: " + f1.getMessage();
            }

            // TODO: instead of throwing, return a JobResult with combined failure
            throw new RuntimeException(message, f1.getThrowable());
        }
    }
}
