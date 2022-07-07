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
package io.bootique.job.group;

import io.bootique.job.Job;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;

import java.util.Map;
import java.util.Objects;

/**
 * @since 3.0
 */
public class SingleJobStep extends JobGroupStep {

    private final Job job;

    public SingleJobStep(Scheduler scheduler, Job job) {
        super(scheduler);
        this.job = Objects.requireNonNull(job);
    }

    @Override
    public JobGroupStepResult run(Map<String, Object> params) {
        JobResult result = scheduler.runBuilder().job(job).params(params).noDecorators().runBlocking();
        logResult(result);

        return result.isSuccess() ? JobGroupStepResult.succeeded(result) : JobGroupStepResult.failed(result);
    }
}