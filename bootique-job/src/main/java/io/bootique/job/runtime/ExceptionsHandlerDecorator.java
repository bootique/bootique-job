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
import io.bootique.job.JobMetadata;
import io.bootique.job.JobDecorator;
import io.bootique.job.JobOutcome;

import java.util.Map;

/**
 * @since 3.0
 */
public class ExceptionsHandlerDecorator implements JobDecorator {

    @Override
    public JobOutcome run(Job delegate, Map<String, Object> params) {
        return runWithExceptionHandling(delegate.getMetadata(), delegate, params);
    }

    // reusable method that can be used by this and other decorators for consistent error handling
    static JobOutcome runWithExceptionHandling(JobMetadata metadata, Job delegate, Map<String, Object> params) {
        try {
            JobOutcome result = delegate.run(params);
            return result != null ? result : JobOutcome.unknown("Job returned null result");
        } catch (Exception e) {
            // not logging the failure here... JobLogger will do the logging
            return JobOutcome.failed(e);
        }
    }

}
