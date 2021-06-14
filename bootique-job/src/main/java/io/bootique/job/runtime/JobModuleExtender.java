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

import io.bootique.ModuleExtender;
import io.bootique.di.Binder;
import io.bootique.di.Key;
import io.bootique.di.SetBuilder;
import io.bootique.di.TypeLiteral;
import io.bootique.job.Job;
import io.bootique.job.JobListener;
import io.bootique.job.MappedJobListener;
import io.bootique.job.lock.LockHandler;

public class JobModuleExtender extends ModuleExtender<JobModuleExtender> {

    private SetBuilder<Job> jobs;
    private SetBuilder<JobListener> listeners;
    private SetBuilder<MappedJobListener> mappedListeners;

    public JobModuleExtender(Binder binder) {
        super(binder);
    }

    public JobModuleExtender initAllExtensions() {
        contributeListeners();
        contributeMappedListeners();
        contributeJobs();

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

    public JobModuleExtender setLockHandler(LockHandler lockHandler) {
        binder.bind(LockHandler.class).toInstance(lockHandler);
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
        return listeners != null ? listeners : (listeners = newSet(JobListener.class));
    }

    protected SetBuilder<MappedJobListener> contributeMappedListeners() {
        return mappedListeners != null ? mappedListeners : (mappedListeners = newSet(MappedJobListener.class));
    }
}
