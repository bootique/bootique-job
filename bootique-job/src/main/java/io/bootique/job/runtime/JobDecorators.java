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
import io.bootique.job.MappedJobDecorator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages job decorators chain.
 *
 * @since 3.0
 */
public class JobDecorators {

    // ordering, outer to inner
    public static final int LOGGER_ORDER = 1000;
    public static final int EXCEPTIONS_HANDLER_ORDER = LOGGER_ORDER + 1000;
    public static final int LOCK_HANDLER_ORDER = EXCEPTIONS_HANDLER_ORDER + 1000;
    public static final int PARAMS_BINDER_ORDER = LOCK_HANDLER_ORDER + 1000;

    /**
     * @deprecated since 3.0 we suggest implementing {@link io.bootique.job.JobListener} as {@link JobDecorator}
     */
    // listeners must be handled after default parameters are applied, so that they can have access to them
    @Deprecated
    public static final int LISTENERS_DISPATCHER_ORDER = PARAMS_BINDER_ORDER + 1000;

    public static final int RENAMER_ORDER = LISTENERS_DISPATCHER_ORDER + 1000;

    //  unordered decorators are always inner
    public static final int UNORDERED_ORDER = RENAMER_ORDER + 1000;

    private final List<JobDecorator> topDecoratorsInnerToOuter;
    private final List<JobDecorator> subDecoratorsInnerToOuter;

    public static Builder builder() {
        return new Builder();
    }

    protected JobDecorators(
            List<JobDecorator> topDecoratorsInnerToOuter,
            List<JobDecorator> subDecoratorsInnerToOuter) {

        this.topDecoratorsInnerToOuter = topDecoratorsInnerToOuter;
        this.subDecoratorsInnerToOuter = subDecoratorsInnerToOuter;
    }

    public Job decorateTopJob(Job job, String altName, Map<String, Object> prebindParams) {
        return decorate(topDecoratorsInnerToOuter, job, altName, prebindParams);
    }

    public Job decorateSubJob(Job job, String altName, Map<String, Object> prebindParams) {
        return decorate(subDecoratorsInnerToOuter, job, altName, prebindParams);
    }

    protected Job decorate(
            List<JobDecorator> decoratorsInnerToOuter,
            Job job,
            String altName,
            Map<String, Object> prebindParams) {

        for (JobDecorator decorator : decoratorsInnerToOuter) {
            job = decorator.decorate(job, altName, prebindParams);
        }

        return job;
    }

    public static class Builder {

        private JobDecorator logger;
        private JobDecorator exceptionHandler;
        private JobDecorator lockHandler;
        private JobDecorator listenerDispatcher;
        private JobDecorator paramsBinder;
        private JobDecorator renamer;
        private final Set<MappedJobDecorator<?>> otherDecorators;
        private final Comparator<MappedJobDecorator<?>> sortInnerToOuter;

        protected Builder() {
            this.otherDecorators = new HashSet<>();

            Comparator<MappedJobDecorator<?>> sortOuterToInner = Comparator.comparing(MappedJobDecorator::getOrder);
            this.sortInnerToOuter = sortOuterToInner.reversed();
        }

        public JobDecorators create() {

            Set<MappedJobDecorator<?>> topDecorators = new HashSet<>(10);
            
            // same as "top", but exclude custom decorators, listeners and lock handlers
            // TODO: would lock handlers actually make sense for child jobs?
            Set<MappedJobDecorator<?>> subDecorators = new HashSet<>(5);

            topDecorators.addAll(otherDecorators);

            if (listenerDispatcher != null) {
                topDecorators.add(new MappedJobDecorator<>(listenerDispatcher, LISTENERS_DISPATCHER_ORDER));
            }

            if (lockHandler != null) {
                topDecorators.add(new MappedJobDecorator<>(lockHandler, LOCK_HANDLER_ORDER));
            }

            if (logger != null) {
                MappedJobDecorator<?> d = new MappedJobDecorator<>(logger, LOGGER_ORDER);
                topDecorators.add(d);
                subDecorators.add(d);
            }

            if (exceptionHandler != null) {
                MappedJobDecorator<?> d = new MappedJobDecorator<>(exceptionHandler, EXCEPTIONS_HANDLER_ORDER);
                topDecorators.add(d);
                subDecorators.add(d);
            }

            if (paramsBinder != null) {
                MappedJobDecorator<?> d = new MappedJobDecorator<>(paramsBinder, PARAMS_BINDER_ORDER);
                topDecorators.add(d);
                subDecorators.add(d);
            }

            if (renamer != null) {
                MappedJobDecorator<?> d = new MappedJobDecorator<>(renamer, RENAMER_ORDER);
                topDecorators.add(d);
                subDecorators.add(d);
            }

            return new JobDecorators(
                    sortedInnerToOuter(topDecorators),
                    sortedInnerToOuter(subDecorators)
            );
        }

        protected List<JobDecorator> sortedInnerToOuter(Set<MappedJobDecorator<?>> mappedUnsorted) {
            return mappedUnsorted.stream()
                    .sorted(sortInnerToOuter)
                    .map(MappedJobDecorator::getDecorator)
                    .collect(Collectors.toList());
        }

        public Builder logger(JobDecorator logger) {
            this.logger = logger;
            return this;
        }

        public Builder exceptionHandler(JobDecorator exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        public Builder paramsBinder(JobDecorator paramsBinder) {
            this.paramsBinder = paramsBinder;
            return this;
        }

        public Builder lockHandler(JobDecorator lockHandler) {
            this.lockHandler = lockHandler;
            return this;
        }

        /**
         * @deprecated since 3.0 as listeners are deprecated, so the listeners' dispatcher is deprecated too
         */
        @Deprecated
        public Builder listenerDispatcher(JobDecorator listenerDispatcher) {
            this.listenerDispatcher = listenerDispatcher;
            return this;
        }

        public Builder renamer(JobDecorator renamer) {
            this.renamer = renamer;
            return this;
        }

        public Builder add(JobDecorator decorator) {
            otherDecorators.add(new MappedJobDecorator<>(decorator, UNORDERED_ORDER));
            return this;
        }

        public Builder add(Collection<JobDecorator> decorators) {
            decorators.forEach(this::add);
            return this;
        }

        public Builder addMapped(MappedJobDecorator<?> decorator) {
            otherDecorators.add(decorator);
            return this;
        }

        public Builder addMapped(Collection<MappedJobDecorator<?>> decorators) {
            decorators.forEach(this::addMapped);
            return this;
        }
    }
}
