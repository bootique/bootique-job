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

import java.util.Set;

public interface JobRegistry {

    /**
     * @deprecated since 3.0 in favor of {@link #getJobNames()}
     */
    default Set<String> getAvailableJobs() {
        return getJobNames();
    }

    /**
     * Returns all known job names, including standalone jobs and job groups.
     *
     * @since 3.0.M1
     */
    Set<String> getJobNames();

    Job getJob(String jobName);

    boolean allowsSimultaneousExecutions(String jobName);
}
