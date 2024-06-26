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

import io.bootique.job.runtime.DecoratedJob;

import java.util.Map;

/**
 * An interceptor that can add behavior to a job during its execution. There are standard decorators that add logging,
 * exception handling, pre-bind default parameters, etc. Users can register their own custom decorators.
 *
 * @since 3.0
 */
@FunctionalInterface
public interface JobDecorator {

    default Job decorate(Job delegate, String altName, Map<String, Object> prebindParams) {
        return isApplicable(delegate.getMetadata(), altName, prebindParams)
                ? new DecoratedJob(delegate, delegate.getMetadata(), this)
                : delegate;
    }

    default boolean isApplicable(JobMetadata metadata, String altName, Map<String, Object> prebindParams) {
        return true;
    }

    JobOutcome run(Job delegate, Map<String, Object> params);
}
