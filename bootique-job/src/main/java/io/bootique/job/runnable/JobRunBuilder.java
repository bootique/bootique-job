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
package io.bootique.job.runnable;

import io.bootique.job.Job;
import io.bootique.job.JobRegistry;
import org.springframework.scheduling.TaskScheduler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * A builder for customizing job execution
 *
 * @since 3.0
 */
public class JobRunBuilder {

    private final JobRegistry registry;
    private final TaskScheduler taskScheduler;
    private final RunnableJobFactory runnableJobFactory;

    private Job job;
    private Map<String, Object> params;

    public JobRunBuilder(
            JobRegistry registry,
            TaskScheduler taskScheduler,
            RunnableJobFactory runnableJobFactory) {
        this.registry = registry;
        this.taskScheduler = taskScheduler;
        this.runnableJobFactory = runnableJobFactory;
    }

    public JobRunBuilder job(Job job) {
        this.job = job;
        return this;
    }

    public JobRunBuilder jobName(String jobName) {
        this.job = registry.getJob(jobName);
        return this;
    }

    public JobRunBuilder params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public JobResult runBlocking() {
        return runnableJobFactory.runnable(resolveJob(), resolveParams()).run();
    }

    public JobFuture runNonBlocking() {

        Job job = resolveJob();
        Map<String, Object> params = resolveParams();

        JobResult[] result = new JobResult[1];
        Future<?> future = taskScheduler.schedule(
                () -> result[0] = runnableJobFactory.runnable(job, params).run(),
                new Date());

        JobFuture f1 = new SimpleJobFuture(
                job.getMetadata().getName(),
                future,
                () -> result[0] != null ? result[0] : JobResult.unknown(job.getMetadata()));

        return f1;
    }

    protected Job resolveJob() {
        return Objects.requireNonNull(job, "Job is not set");
    }

    protected Map<String, Object> resolveParams() {
        // using a mutable map, as job listeners can change parameters
        return this.params != null ? this.params : new HashMap<>();
    }
}
