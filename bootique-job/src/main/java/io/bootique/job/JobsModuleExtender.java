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

import io.bootique.ModuleExtender;
import io.bootique.di.*;
import io.bootique.job.lock.LockHandler;

import java.util.function.Consumer;

/**
 * @since 3.0
 */
public class JobsModuleExtender extends ModuleExtender<JobsModuleExtender> {

    private SetBuilder<Job> jobs;
    private SetBuilder<JobDecorator> decorators;
    private SetBuilder<MappedJobDecorator<?>> mappedDecorators;
    private SetBuilder<LockHandler> lockHandlers;

    public JobsModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public JobsModuleExtender initAllExtensions() {
        contributeDecorators();
        contributeMappedDecorators();
        contributeJobs();
        contributeLockHandlers();

        return this;
    }

    /**
     * Syntactic sugar for extender configuration that allows e.g. to load multiple jobs from a collection without
     * assigning extender to a variable.
     *
     * @since 3.0
     */
    public JobsModuleExtender config(Consumer<JobsModuleExtender> configurator) {
        configurator.accept(this);
        return this;
    }

    public JobsModuleExtender addJob(Job job) {
        contributeJobs().addInstance(job);
        return this;
    }

    public JobsModuleExtender addJob(Class<? extends Job> jobType) {
        // TODO: what does singleton scope means when adding to collection?
        contributeJobs().add(jobType);
        return this;
    }

    /**
     * @since 3.0
     */
    public JobsModuleExtender setLockHandler(LockHandler handler) {
        contributeLockHandlers().addInstance(handler);
        return this;
    }

    /**
     * @since 3.0
     */
    public JobsModuleExtender setLockHandler(Class<? extends LockHandler> handlerType) {
        contributeLockHandlers().add(handlerType);
        return this;
    }

    /**
     * @since 3.0
     */
    public <T extends JobDecorator> JobsModuleExtender addMappedDecorator(MappedJobDecorator<T> mappedJobDecorator) {
        contributeMappedDecorators().addInstance(mappedJobDecorator);
        return this;
    }

    /**
     * @since 3.0
     */
    public <T extends JobDecorator> JobsModuleExtender addMappedDecorator(Key<MappedJobDecorator<T>> mappedJobDecoratorKey) {
        contributeMappedDecorators().add(mappedJobDecoratorKey);
        return this;
    }

    /**
     * @since 3.0
     */
    public <T extends JobDecorator> JobsModuleExtender addMappedDecorator(TypeLiteral<MappedJobDecorator<T>> mappedJobDecoratorType) {
        return addMappedDecorator(Key.get(mappedJobDecoratorType));
    }

    /**
     * @since 3.0
     */
    public JobsModuleExtender addDecorator(JobDecorator decorator) {
        contributeDecorators().addInstance(decorator);
        return this;
    }

    /**
     * @since 3.0
     */
    public JobsModuleExtender addDecorator(Class<? extends JobDecorator> type) {
        contributeDecorators().add(type);
        return this;
    }

    /**
     * @since 3.0
     */
    public JobsModuleExtender addDecorator(JobDecorator decorator, int order) {
        return addMappedDecorator(new MappedJobDecorator<>(decorator, order));
    }

    protected SetBuilder<Job> contributeJobs() {
        return jobs != null ? jobs : (jobs = newSet(Job.class));
    }

    protected SetBuilder<JobDecorator> contributeDecorators() {
        if (decorators == null) {
            decorators = newSet(JobDecorator.class);
        }
        return decorators;
    }

    protected SetBuilder<MappedJobDecorator<?>> contributeMappedDecorators() {
        if (mappedDecorators == null) {
            mappedDecorators = binder.bindSet(new TypeLiteral<>() {
            });
        }
        return mappedDecorators;
    }

    protected SetBuilder<LockHandler> contributeLockHandlers() {
        if (lockHandlers == null) {
            lockHandlers = newSet(LockHandler.class);
        }
        return lockHandlers;
    }
}
