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
import io.bootique.job.JobDecorator;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages job decorator chain.
 *
 * @since 3.0
 */
public class JobDecorators {

    // ordering, outer to inner
    public static final int LOGGER_DECORATOR_ORDER = 1000;
    public static final int JOB_EXCEPTIONS_HANDLER_DECORATOR_ORDER = LOGGER_DECORATOR_ORDER + 1000;
    public static final int LOCK_HANDLER_DECORATOR_ORDER = JOB_EXCEPTIONS_HANDLER_DECORATOR_ORDER + 1000;
    public static final int PARAM_DEFAULTS_DECORATOR_ORDER = LOCK_HANDLER_DECORATOR_ORDER + 1000;
    // listeners must be handled after default parameters are applied, so that they can have access to them
    public static final int LISTENERS_DECORATOR_ORDER = PARAM_DEFAULTS_DECORATOR_ORDER + 1000;
    public static final int JOB_NAME_DECORATOR_ORDER = LISTENERS_DECORATOR_ORDER + 1000;

    private final List<JobDecorator> decoratorsInnerToOuter;
    private final JobDecorator exceptionHandler;

    public JobDecorators(List<JobDecorator> decoratorsInnerToOuter, JobDecorator exceptionHandler) {
        this.decoratorsInnerToOuter = Objects.requireNonNull(decoratorsInnerToOuter);
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler);
    }

    public Job decorate(Job job, String altName, Map<String, Object> prebindParams) {
        for (JobDecorator decorator : decoratorsInnerToOuter) {
            job = decorator.decorate(job, altName, prebindParams);
        }

        return decorateWithExceptionHandler(job, altName, prebindParams);
    }

    public Job decorateWithExceptionHandler(Job job, String altName, Map<String, Object> prebindParams) {
        return exceptionHandler.decorate(job, altName, prebindParams);
    }
}
