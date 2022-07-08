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

public class JobModuleExtender extends ModuleExtender<JobModuleExtender> {

    private SetBuilder<Job> jobs;
    private SetBuilder<JobListener> listeners;
    private SetBuilder<MappedJobListener> mappedListeners;
    private SetBuilder<JobDecorator> decorators;
    private SetBuilder<MappedJobDecorator> mappedDecorators;
    private SetBuilder<LockHandler> lockHandlers;

    public JobModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public JobModuleExtender initAllExtensions() {
        contributeListeners();
        contributeMappedListeners();
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
    public JobModuleExtender config(Consumer<JobModuleExtender> configurator) {
        configurator.accept(this);
        return this;
    }

    public JobModuleExtender addJob(Job job) {
        contributeJobs().addInstance(job);
        return this;
    }

    public JobModuleExtender addJob(Class<? extends Job> jobType) {
        // TODO: what does singleton scope means when adding to collection?
        contributeJobs().add(jobType);
        return this;
    }

    /**
     * @since 3.0.M1
     */
    public JobModuleExtender setLockHandler(LockHandler handler) {
        contributeLockHandlers().addInstance(handler);
        return this;
    }

    /**
     * @since 3.0.M1
     */
    public JobModuleExtender setLockHandler(Class<? extends LockHandler> handlerType) {
        contributeLockHandlers().add(handlerType);
        return this;
    }

    /**
     * @deprecated since 3.0 we suggest implementing {@link io.bootique.job.JobListener} as {@link JobDecorator} and use
     * {@link #addMappedDecorator(MappedJobDecorator)}
     */
    @Deprecated
    public <T extends JobListener> JobModuleExtender addMappedListener(MappedJobListener<T> mappedJobListener) {
        contributeMappedListeners().addInstance(mappedJobListener);
        return this;
    }

    /**
     * @deprecated since 3.0 we suggest implementing {@link io.bootique.job.JobListener} as {@link JobDecorator} and use
     * {@link #addMappedDecorator(Key)}
     */
    @Deprecated
    public <T extends JobListener> JobModuleExtender addMappedListener(Key<MappedJobListener<T>> mappedJobListenerKey) {
        contributeMappedListeners().add(mappedJobListenerKey);
        return this;
    }

    /**
     * @deprecated since 3.0 we suggest implementing {@link io.bootique.job.JobListener} as {@link JobDecorator} and use
     * {@link #addMappedDecorator(TypeLiteral)}
     */
    @Deprecated
    public <T extends JobListener> JobModuleExtender addMappedListener(TypeLiteral<MappedJobListener<T>> mappedJobListenerType) {
        contributeMappedListeners().add(Key.get(mappedJobListenerType));
        return this;
    }

    /**
     * @deprecated since 3.0 we suggest implementing {@link io.bootique.job.JobListener} as {@link JobDecorator} and use
     * {@link #addDecorator(Class)}
     */
    @Deprecated
    public JobModuleExtender addListener(Class<? extends JobListener> listenerType) {
        // TODO: what does singleton scope means when adding to collection?
        contributeListeners().add(listenerType);
        return this;
    }

    /**
     * @deprecated since 3.0 we suggest implementing {@link io.bootique.job.JobListener} as {@link JobDecorator} and use
     * {@link #addDecorator(JobDecorator)}.
     */
    @Deprecated
    public JobModuleExtender addListener(JobListener listener) {
        contributeListeners().addInstance(listener);
        return this;
    }

    /**
     * @since 3.0
     */
    public <T extends JobDecorator> JobModuleExtender addMappedDecorator(MappedJobDecorator<T> mappedJobDecorator) {
        contributeMappedDecorators().addInstance(mappedJobDecorator);
        return this;
    }

    /**
     * @since 3.0
     */
    public <T extends JobDecorator> JobModuleExtender addMappedDecorator(Key<MappedJobDecorator<T>> mappedJobDecoratorKey) {
        contributeMappedDecorators().add(mappedJobDecoratorKey);
        return this;
    }

    /**
     * @since 3.0
     */
    public <T extends JobDecorator> JobModuleExtender addMappedDecorator(TypeLiteral<MappedJobDecorator<T>> mappedJobDecoratorType) {
        return addMappedDecorator(Key.get(mappedJobDecoratorType));
    }

    /**
     * @since 3.0
     */
    public JobModuleExtender addDecorator(JobDecorator decorator) {
        contributeDecorators().addInstance(decorator);
        return this;
    }

    /**
     * @since 3.0
     */
    public JobModuleExtender addDecorator(Class<? extends JobDecorator> type) {
        contributeDecorators().add(type);
        return this;
    }

    /**
     * @since 3.0
     */
    public JobModuleExtender addDecorator(JobDecorator decorator, int order) {
        return addMappedDecorator(new MappedJobDecorator<>(decorator, order));
    }

    protected SetBuilder<Job> contributeJobs() {
        return jobs != null ? jobs : (jobs = newSet(Job.class));
    }

    protected SetBuilder<JobListener> contributeListeners() {
        if (listeners == null) {
            listeners = newSet(JobListener.class);
        }
        return listeners;
    }

    protected SetBuilder<MappedJobListener> contributeMappedListeners() {
        if (mappedListeners == null) {
            mappedListeners = newSet(MappedJobListener.class);
        }
        return mappedListeners;
    }

    protected SetBuilder<JobDecorator> contributeDecorators() {
        if (decorators == null) {
            decorators = newSet(JobDecorator.class);
        }
        return decorators;
    }

    protected SetBuilder<MappedJobDecorator> contributeMappedDecorators() {
        if (mappedDecorators == null) {
            mappedDecorators = newSet(MappedJobDecorator.class);
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
