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

public interface Scheduler {

    /**
     * Returns a builder object that can be used to build a custom job execution.
     *
     * @since 3.0
     */
    JobRunBuilder runBuilder();

    /**
     * Schedule execution of jobs based on configured triggers. Throws an exception if the scheduler has already been started
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
