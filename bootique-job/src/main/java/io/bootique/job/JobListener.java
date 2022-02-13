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

import io.bootique.job.runnable.JobResult;

import java.util.Map;
import java.util.function.Consumer;

/**
 * A listener that will be notified of every started job. When a job is started the listener may optionally decide to
 * get notified when this particular job is finished by registering a callback function with provided event source.
 */
public interface JobListener {

    /**
     * A method invoked when a job is started. The listener may optionally decide to get notified when the job is
     * finished by registering a callback function with provided "onFinishedCallbackRegistry".
     *
     * @param jobName           the name of a job that generated start event.
     * @param parameters        parameters passed to the job.
     * @param onFinishedCallbackRegistry an object that will notify registered consumers when the job that generated this start
     *                          event is finished.
     */
    void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry);
}
