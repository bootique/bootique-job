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

package io.bootique.job.scheduler.execution.group;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * A stateful execution object for running job group batches, with an ability to yield and resume the group thread
 * to avoid thread pool deadlocks.
 *
 * @since 3.0
 */
public class JobGroupNonBlockingExecution extends BaseJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobGroupNonBlockingExecution.class);

    private final Scheduler scheduler;
    private final List<JobGroupStep> remainingSteps;
    private final List<JobFuture> stepInProgress;

    public JobGroupNonBlockingExecution(
            JobMetadata metadata,
            Scheduler scheduler,
            List<JobFuture> stepInProgress,
            List<JobGroupStep> remainingSteps) {

        super(metadata);
        this.scheduler = scheduler;
        this.stepInProgress = stepInProgress;
        this.remainingSteps = remainingSteps;
    }

    @Override
    public JobResult run(Map<String, Object> params) {

        LOGGER.debug("{} woke up", getMetadata().getName());

        if (checkStillInProgress()) {
            // TODO: schedule a small delay before resume?
            return yield(params);
        }

        for (int i = 0; i < remainingSteps.size(); i++) {
            JobGroupStepResult result = remainingSteps.get(i).run(params);
            switch (result.getOutcome()) {

                case failed:
                    return result.getJobResult();
                case yielded:
                    return new JobGroupNonBlockingExecution(
                            getMetadata(),
                            scheduler,
                            result.getStepInProgress(),
                            remainingSteps.subList(i + 1, remainingSteps.size())).yield(params);
                case succeeded:
                default:
                    continue;
            }
        }

        return JobResult.success(getMetadata());
    }

    protected JobResult yield(Map<String, Object> params) {
        JobFuture yieldedTo = scheduler.runOnce(this, params);
        return JobResult.yielded(getMetadata(), yieldedTo);
    }

    protected boolean checkStillInProgress() {

        for (JobFuture f : stepInProgress) {
            if (!f.isDone()) {
                return true;
            }
        }
        return false;
    }
}
