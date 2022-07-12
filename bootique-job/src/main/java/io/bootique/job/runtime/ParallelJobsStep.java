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
import io.bootique.job.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @since 3.0
 */
public class ParallelJobsStep extends GraphJobStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelJobsStep.class);

    private final GraphExecutor executor;
    private final List<Job> jobs;

    public ParallelJobsStep(GraphExecutor executor, List<Job> jobs) {
        this.executor = executor;
        this.jobs = Objects.requireNonNull(jobs);
    }

    @Override
    public JobResult run(Map<String, Object> params) {

        List<Future<JobResult>> submitted = jobs
                .stream()
                .skip(1)
                .map(j -> executor.submit(j, params))
                .collect(Collectors.toList());

        JobResult r0 = jobs.get(0).run(params);
        logResult(r0);

        if (!r0.isSuccess()) {
            LOGGER.debug("First job '{}' failed, canceling the remaining ones", r0.getMetadata().getName());
            cancelAll(submitted);
            return r0;
        }

        for (int i = 0; i < submitted.size(); i++) {

            JobResult r;
            try {
                r = submitted.get(i).get();
            } catch (ExecutionException | InterruptedException e) {
                r = JobResult.failure(jobs.get(i).getMetadata(), e);
            }

            logResult(r);

            if (!r.isSuccess()) {
                if (i + 1 < submitted.size()) {
                    LOGGER.debug("Job '{}' failed, canceling the remaining ones", r.getMetadata().getName());
                    cancelAll(submitted.subList(i + 1, submitted.size()));
                }

                return r;
            }
        }

        return r0;
    }

    private void cancelAll(List<Future<JobResult>> tasks) {
        tasks.forEach(t -> t.cancel(true));
    }
}
