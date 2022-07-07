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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @since 3.0
 */
public class ErrorHandlingJobDecorator implements JobDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlingJobDecorator.class);

    @Override
    public JobResult run(Job delegate, Map<String, Object> params) {
        try {
            return delegate.run(params);
        } catch (Throwable th) {
            LOGGER.info("Exception while running job '{}'", delegate.getMetadata().getName(), th);
            return JobResult.failure(delegate.getMetadata(), th);
        }
    }
}