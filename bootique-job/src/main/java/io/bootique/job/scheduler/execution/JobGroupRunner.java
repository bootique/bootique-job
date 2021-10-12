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
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class JobGroupRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobGroupRunner.class);

    private final Scheduler scheduler;
    private final JobMetadata groupMetadata;
    private final Map<String, Job> jobs;

    JobGroupRunner(Scheduler scheduler, JobMetadata groupMetadata, Map<String, Job> jobs) {
        this.scheduler = scheduler;
        this.groupMetadata = groupMetadata;
        this.jobs = jobs;
    }

    void execute(Set<JobExecution> jobExecutions, Map<String, Object> runParams) {

        if (jobExecutions.isEmpty()) {
            JobResult.failure(groupMetadata, "No jobs");
        }

        Set<JobResult> failures = new HashSet<>();

        jobExecutions.stream()
                .map(e -> runJob(e, runParams))
                .map(JobFuture::get)
                .forEach(r -> processJobResult(r, failures));

        if (!failures.isEmpty()) {

            JobResult f1 = failures.iterator().next();

            String message = "Failed to execute job: " + f1.getMetadata().getName();
            if (f1.getMessage() != null) {
                message += ". Reason: " + f1.getMessage();
            }
            throw new RuntimeException(message, f1.getThrowable());
        }
    }

    private JobFuture runJob(JobExecution execution, Map<String, Object> runParams) {
        Job job = jobs.get(execution.getJobName());
        return scheduler.runOnce(job, mergeParams(runParams, execution.getParams()));
    }

    private void processJobResult(JobResult result, Set<JobResult> failures) {
        if (result.getThrowable() == null) {
            LOGGER.info(String.format("Finished job '%s', result: %s, message: %s",
                    result.getMetadata().getName(),
                    result.getOutcome(),
                    result.getMessage()));
        } else {
            LOGGER.error(String.format("Finished job '%s', result: %s, message: %s",
                            result.getMetadata().getName(),
                            result.getOutcome(),
                            result.getMessage()),
                    result.getThrowable());
        }

        if (result.getOutcome() != JobOutcome.SUCCESS) {
            failures.add(result);
        }
    }

    private Map<String, Object> mergeParams(Map<String, Object> overridingParams, Map<String, Object> defaultParams) {
        Map<String, Object> merged = new HashMap<>(defaultParams);
        merged.putAll(overridingParams);
        return merged;
    }
}
