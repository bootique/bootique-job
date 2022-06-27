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

package io.bootique.job.scheduler;

import io.bootique.job.runnable.JobFuture;

public interface ScheduledJobFuture extends JobFuture {

    /**
     * Reschedule this job based on the provided schedule. Has no effect, if the job has already been scheduled and
     * hasn't finished yet.
     */
    boolean schedule(Trigger trigger);

    /**
     * @return true, if this has been scheduled and has not finished or been cancelled yet
     */
    boolean isScheduled();

    /**
     * @return Schedule, or throws if {@link #isScheduled()} is false
     */
    Trigger getTrigger();
}
