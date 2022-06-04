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
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @since 3.0
 */
public class ParallelJobBatchStep extends JobGroupStep {

    private final List<Job> jobs;
    private final JobMetadata metadata;

    public ParallelJobBatchStep(
            Scheduler scheduler,
            List<Job> jobs,
            JobMetadata metadata) {

        super(scheduler);
        this.jobs = Objects.requireNonNull(jobs);
        this.metadata = Objects.requireNonNull(metadata);
    }

    @Override
    public JobMetadata getMetadata() {
        return metadata;
    }

    @Override
    public JobResult run(Map<String, Object> params) {

        // schedule 2...N jobs in the background (non-blocking)
        // to ensure parallel execution, must collect futures in an explicit collection,
        // and then "get" them in a separate stream / loop
        List<JobFuture> futures = jobs.stream()
                .skip(1)
                .map(j -> submitGroupMember(j, params))
                .collect(Collectors.toList());

        // run the first job on the group thread (blocking)
        JobResult blockingResult = scheduler.runOnceBlocking(jobs.get(0), params);
        logResult(blockingResult);

        if (!blockingResult.isSuccess()) {
            // TODO: don't bother to cancel other running jobs?
            return blockingResult;
        }

        //  collect results from other jobs' futures (blocking)
        for (JobFuture f : futures) {
            JobResult result = f.get();
            logResult(result);

            // TODO: don't bother to cancel other running jobs?
            if (!result.isSuccess()) {
                return result;
            }
        }

        return JobResult.success(getMetadata());
    }

    protected JobFuture submitGroupMember(Job job, Map<String, Object> params) {
        return scheduler.runOnce(job, params);
    }
}