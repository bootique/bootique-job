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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface JobFuture extends ScheduledFuture<JobResult> {

    static JobFutureBuilder forJob(String job) {
        return new JobFutureBuilder(job);
    }

    /**
     * @return Job name
     */
    String getJobName();

    /**
     * Waits till the job is done and then returns the result.
     */
    @Override
    JobResult get();

    /**
     * Waits till the job is done and then returns the result.
     * Throws an exception, if time elapses before the job has finished.
     */
    @Override
    JobResult get(long timeout, TimeUnit unit);

    /**
     * Convenient shortcut for {@link java.util.concurrent.Future#cancel(boolean)}.
     *
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally;
     * {@code true} otherwise
     */
    default boolean cancelInterruptibly() {
        return cancel(true);
    }
}
