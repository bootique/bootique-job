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

import java.util.Map;

public interface RunnableJobFactory {

    /**
     * Creates a {@link RunnableJob} object that combines job instance with a set of parameters.
     *
     * @param job        A job instance to run when the returned {@link RunnableJob} is executed.
     * @param parameters A set of parameters to apply to the job when the returned {@link RunnableJob} is executed.
     * @return a wrapper around a job and a set of parameters.
     */
    RunnableJob runnable(Job job, Map<String, Object> parameters);
}
