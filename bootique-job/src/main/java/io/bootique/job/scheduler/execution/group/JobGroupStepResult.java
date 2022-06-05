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

import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @since 3.0
 */
public class JobGroupStepResult {

    private final JobGroupStepOutcome outcome;
    private final JobResult jobResult;
    private final List<JobFuture> stepInProgress;

    public static JobGroupStepResult succeeded(JobResult jobResult) {
        return new JobGroupStepResult(JobGroupStepOutcome.succeeded, jobResult, Collections.emptyList());
    }

    public static JobGroupStepResult failed(JobResult jobResult) {
        return new JobGroupStepResult(JobGroupStepOutcome.failed, jobResult, Collections.emptyList());
    }

    public static JobGroupStepResult yielded(List<JobFuture> stepInProgress) {
        return new JobGroupStepResult(JobGroupStepOutcome.yielded, null, stepInProgress);
    }

    private JobGroupStepResult(JobGroupStepOutcome outcome, JobResult jobResult, List<JobFuture> stepInProgress) {
        this.outcome = Objects.requireNonNull(outcome);
        this.jobResult = jobResult;
        this.stepInProgress = stepInProgress;
    }

    public JobGroupStepOutcome getOutcome() {
        return outcome;
    }

    public JobResult getJobResult() {
        return jobResult;
    }

    public List<JobFuture> getStepInProgress() {
        return stepInProgress;
    }
}
