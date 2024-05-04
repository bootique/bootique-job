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
import io.bootique.job.JobOutcome;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A secondary thread pool to run graph subtasks without deadlocking the main pool used by the Scheduler.
 *
 * @since 3.0
 */
public class GraphExecutor {

    private final ExecutorService pool;

    public GraphExecutor(ExecutorService pool) {
        this.pool = pool;
    }

    public Future<JobOutcome> submit(Job job, Map<String, Object> params) {
        return pool.submit(new CallableJob(job, params));
    }

    static class CallableJob implements Callable<JobOutcome> {
        final Job job;
        final Map<String, Object> params;

        CallableJob(Job job, Map<String, Object> params) {
            this.job = job;
            this.params = params;
        }

        @Override
        public JobOutcome call() {
            return job.run(params);
        }
    }
}
