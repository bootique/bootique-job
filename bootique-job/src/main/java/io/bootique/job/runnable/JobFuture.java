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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A specialized future for a single job execution that hides checked exceptions and provides job execution result.
 */
public interface JobFuture extends Future<JobResult> {

    static JobFutureBuilder forJob(String job) {
        return new JobFutureBuilder(job);
    }

    String getJobName();

    /**
     * Waits till the job is done and then returns the result.
     */
    // override super to hide checked exceptions
    @Override
    JobResult get();

    /**
     * Waits till the job is done and then returns the result. Throws an exception, if timeout elapses before the job
     * has finished.
     */
    // override super to hide checked exceptions
    @Override
    JobResult get(long timeout, TimeUnit unit);
}
