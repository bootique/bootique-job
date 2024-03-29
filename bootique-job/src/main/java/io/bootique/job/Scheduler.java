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

import io.bootique.BootiqueException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Scheduler {

    /**
     * Executes a given job once. The method does not block to wait for the job to finish. Instead, it returns a
     * future object to track job progress.
     *
     * @param jobName the name of the job to execute.
     * @return a Future to track job progress.
     * @deprecated since 3.0 in favor of {@link #runBuilder()}
     */
    @Deprecated
    default JobFuture runOnce(String jobName) {
        return runBuilder().jobName(jobName).runNonBlocking();
    }

    /**
     * Executes a given job once.  The method does not block to wait for the job to finish. Instead, it returns a
     * future object to track job progress.
     *
     * @param jobName the name of the job to execute.
     * @param params  a Map of parameters that will be merged with the DI-provided parameters for this execution.
     * @return a Future to track job progress.
     * @deprecated since 3.0 in favor of {@link #runBuilder()}
     */
    @Deprecated
    default JobFuture runOnce(String jobName, Map<String, Object> params) {
        return runBuilder().jobName(jobName).params(params).runNonBlocking();
    }

    /**
     * Executes a given job once.  The method does not block to wait for the job to finish. Instead, it returns a
     * future object to track job progress.
     *
     * @param job Job to execute
     * @return a Future to track job progress.
     * @deprecated since 3.0 in favor of {@link #runBuilder()}
     */
    @Deprecated
    default JobFuture runOnce(Job job) {
        return runBuilder().job(job).runNonBlocking();
    }

    /**
     * Executes a given job once.  The method does not block to wait for the job to finish. Instead, it returns a
     * future object to track job progress.
     *
     * @param job    Job to execute
     * @param params a Map of parameters that will be merged with the DI-provided parameters for this execution.
     * @return a Future to track job progress.
     * @deprecated since 3.0 in favor of {@link #runBuilder()}
     */
    @Deprecated
    default JobFuture runOnce(Job job, Map<String, Object> params) {
        return runBuilder().job(job).params(params).runNonBlocking();
    }

    /**
     * Returns a builder object that can be used to build a custom job execution.
     *
     * @since 3.0
     */
    JobRunBuilder runBuilder();

    /**
     * Schedule execution of jobs based on configured triggers.
     * Throws an exception, if the scheduler has already been started
     *
     * @return Number of scheduled jobs, possibly zero
     */
    int start();

    /**
     * Schedule execution of jobs based on configured triggers.
     * Throws an exception, if the scheduler has already been started
     *
     * @param jobNames Jobs to schedule
     * @return Number of scheduled jobs, possibly zero
     * @throws BootiqueException if {@code jobNames} is null or empty or some of the jobs are unknown
     */
    int start(List<String> jobNames);

    /**
     * @return true, if the scheduler has been started
     */
    boolean isStarted();

    /**
     * @return Collection of scheduled job executions for all known jobs
     */
    Collection<ScheduledJob> getScheduledJobs();

    /**
     * @param jobName Job name
     * @return Scheduled job executions for a given job, or an empty collection, if the job is unknown,
     * triggers are not configured for this job or the scheduler has not been started yet
     */
    Collection<ScheduledJob> getScheduledJobs(String jobName);
}
