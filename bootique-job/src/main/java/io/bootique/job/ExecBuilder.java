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

import io.bootique.job.runtime.JobDecorators;
import io.bootique.job.runtime.SimpleJobFuture;
import io.bootique.job.scheduler.TaskScheduler;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * A builder for a customized job execution.
 *
 * @since 4.0
 */
public class ExecBuilder {

    private final JobRegistry registry;
    private final TaskScheduler taskScheduler;
    private final JobDecorators decorators;

    private Job job;
    private String jobName;
    private Map<String, Object> params;
    private boolean noDecorators;

    public ExecBuilder(
            JobRegistry registry,
            TaskScheduler taskScheduler,
            JobDecorators decorators) {
        this.registry = registry;
        this.taskScheduler = taskScheduler;
        this.decorators = decorators;
    }

    public ExecBuilder job(Job job) {
        this.job = job;
        this.jobName = null;
        return this;
    }

    public ExecBuilder jobName(String jobName) {
        this.job = null;
        this.jobName = jobName;
        return this;
    }

    public ExecBuilder params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public ExecBuilder noDecorators() {
        this.noDecorators = true;
        return this;
    }

    /**
     * Runs the specified job, blocking until it finishes.
     */
    public JobOutcome run() {
        return resolveJob().run(resolveParams());
    }

    /**
     * @deprecated in favor of {@link #run()}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public JobOutcome runBlocking() {
        return run();
    }

    /**
     * Schedules a single run the specified job, returning immediately with a Future object that can be used
     * by the caller to see the job through to completion.
     */
    public JobFuture runNonBlocking() {

        Job job = resolveJob();
        Map<String, Object> params = resolveParams();

        JobOutcome[] result = new JobOutcome[1];
        Future<?> future = taskScheduler.schedule(() -> result[0] = job.run(params), Instant.now());

        return new SimpleJobFuture(
                job.getMetadata().getName(),
                future,
                () -> result[0] != null ? result[0] : JobOutcome.unknown());
    }

    protected Job resolveJob() {
        if (jobName != null) {
            // registry jobs are already decorated. No explicit decorators need to apply
            return registry.getJob(jobName);
        }

        if (job != null) {
            return noDecorators
                    ? job
                    : decorators.decorateTopJob(job, null, Collections.emptyMap());
        }

        throw new IllegalStateException("Neither 'job' nor 'jobName' are set");
    }

    protected Map<String, Object> resolveParams() {
        // using a mutable map, as job listeners can change parameters
        return this.params != null ? this.params : new HashMap<>();
    }
}
