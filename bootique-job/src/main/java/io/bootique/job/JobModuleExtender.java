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

import javax.inject.Provider;
import java.util.function.Consumer;

public class JobModuleExtender extends ModuleExtender<JobModuleExtender> {

    private SetBuilder<Job> jobs;
    private SetBuilder<JobListener> listeners;
    private SetBuilder<MappedJobListener> mappedListeners;
    private MapBuilder<String, LockHandler> lockHandlers;

    public JobModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public JobModuleExtender initAllExtensions() {
        contributeListeners();
        contributeMappedListeners();
        contributeJobs();
        contributeLockHandlers();

        return this;
    }

    /**
     * Provides syntactic sugar for extender configuration, allowing e.g. to load multiple jobs from a collection
     * without assigning extender to a variable.
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

    public JobModuleExtender addListener(JobListener listener) {
        contributeListeners().addInstance(listener);
        return this;
    }

    /**
     * @since 3.0.M1
     */
    public JobModuleExtender addLockHandler(String name, LockHandler handler) {
        contributeLockHandlers().putInstance(name, handler);
        return this;
    }

    /**
     * @since 3.0.M1
     */
    public JobModuleExtender addLockHandler(String name, Class<? extends LockHandler> handlerType) {
        contributeLockHandlers().put(name, handlerType);
        return this;
    }

    /**
     * @param lockHandler class that implements LockHandler
     * @return this
     */
    public JobModuleExtender setLockHandler(Class<? extends LockHandler> lockHandler) {
        binder.bind(LockHandler.class).to(lockHandler);
        return this;
    }

    /**
     * @param lockHandler key of the LockHandler implementation
     * @return this
     */
    public JobModuleExtender setLockHandler(Key<? extends LockHandler> lockHandler) {
        binder.bind(LockHandler.class).to(lockHandler);
        return this;
    }

    /**
     * @param lockHandlerProvider LockHandler provider
     * @return this
     */
    public JobModuleExtender setLockHandlerProvider(Class<Provider<? extends LockHandler>> lockHandlerProvider) {
        binder.bind(LockHandler.class).toProvider(lockHandlerProvider);
        return this;
    }

    /**
     * @param lockHandlerProvider LockHandler provider
     * @return this
     */
    public JobModuleExtender setLockHandlerProvider(Provider<? extends LockHandler> lockHandlerProvider) {
        binder.bind(LockHandler.class).toProviderInstance(lockHandlerProvider);
        return this;
    }

    /**
     * Adds a listener to the set of Job listeners.
     *
     * @param mappedJobListener a wrapped listener
     * @param <T>
     * @return this extender instance
     */
    public <T extends JobListener> JobModuleExtender addMappedListener(MappedJobListener<T> mappedJobListener) {
        contributeMappedListeners().addInstance(mappedJobListener);
        return this;
    }

    /**
     * Adds a listener of the specified type to the set of Job listeners.
     *
     * @param mappedJobListenerKey binding key
     * @param <T>
     * @return this extender instance
     */
    public <T extends JobListener> JobModuleExtender addMappedListener(Key<MappedJobListener<T>> mappedJobListenerKey) {
        contributeMappedListeners().add(mappedJobListenerKey);
        return this;
    }

    /**
     * Adds a listener of the specified type to the set of Job listeners.
     *
     * @param mappedJobListenerType listener type
     * @param <T>
     * @return this extender instance
     */
    public <T extends JobListener> JobModuleExtender addMappedListener(TypeLiteral<MappedJobListener<T>> mappedJobListenerType) {
        contributeMappedListeners().add(Key.get(mappedJobListenerType));
        return this;
    }

    public JobModuleExtender addListener(Class<? extends JobListener> listenerType) {
        // TODO: what does singleton scope means when adding to collection?
        contributeListeners().add(listenerType);
        return this;
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

    protected MapBuilder<String, LockHandler> contributeLockHandlers() {
        if (lockHandlers == null) {
            lockHandlers = newMap(String.class, LockHandler.class);
        }
        return lockHandlers;
    }
}
