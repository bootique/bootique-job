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
package io.bootique.job.fixture;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BaseTestJob<T extends BaseTestJob<T>> extends BaseJob {

    private Map<String, Object> params;
    private long executedAtNanos;

    protected BaseTestJob(Class<T> jobType) {
        super(JobMetadata.build(jobType));
    }

    protected BaseTestJob(JobMetadata metadata) {
        super(metadata);
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        this.executedAtNanos = System.nanoTime();
        this.params = params;
        return JobResult.success(getMetadata());
    }

    public T assertNotExecuted() {
        assertTrue(executedAtNanos == 0L, () -> "Job " + getMetadata().getName() + " was unexpectedly executed");
        return (T) this;
    }

    public T assertExecuted() {
        assertTrue(executedAtNanos > 0, () -> "Job " + getMetadata().getName() + " was not executed");
        return (T) this;
    }

    public T assertExecuted(Map<String, Object> params) {
        assertExecuted();
        assertEquals(params, this.params, "Unexpected execution parameters");
        return (T) this;
    }

    public T assertExecutedBefore(BaseTestJob<?> anotherJob) {

        this.assertExecuted();
        anotherJob.assertExecuted();
        assertTrue(this.executedAtNanos < anotherJob.executedAtNanos, () ->
                "Unexpected order of job execution: " + getMetadata().getName() + " was not executed before " + anotherJob.getMetadata().getName());

        return (T) this;
    }
}
