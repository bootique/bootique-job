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
import io.bootique.job.MappedJobListener;
import io.bootique.job.JobDecorator;
import io.bootique.job.JobOutcome;

import java.util.*;
import java.util.function.Consumer;

/**
 * @since 3.0
 * @deprecated since 3.0, as each {@link io.bootique.job.JobListener} can be easily converted into a {@link JobDecorator}
 */
@Deprecated
public class JobListenersDispatcherDecorator implements JobDecorator {

    private final Collection<MappedJobListener> listeners;

    public JobListenersDispatcherDecorator(Collection<MappedJobListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public boolean isApplicable(JobMetadata metadata, String altName, Map<String, Object> prebindParams) {
        return !listeners.isEmpty();
    }

    @Override
    public JobOutcome run(Job delegate, Map<String, Object> params) {

        String jobName = delegate.getMetadata().getName();
        JobListenerInvoker listenerInvoker = new JobListenerInvoker(jobName);
        listenerInvoker.onStart(listeners, params);

        JobOutcome result = ExceptionsHandlerDecorator.runWithExceptionHandling(delegate.getMetadata(), delegate, params);

        // invoke outside try/catch... Listener exceptions will be processed downstream
        listenerInvoker.onFinish(result);
        return result;
    }

    static class JobListenerInvoker implements Consumer<Consumer<JobOutcome>> {

        private final String jobName;
        private final List<Consumer<JobOutcome>> callbacks;

        JobListenerInvoker(String jobName) {
            this.jobName = jobName;
            this.callbacks = new ArrayList<>(2);
        }

        @Override
        public void accept(Consumer<JobOutcome> callback) {
            callbacks.add(callback);
        }

        public void onStart(Collection<MappedJobListener> listeners, Map<String, Object> parameters) {
            listeners.stream().map(MappedJobListener::getListener).forEach(l -> l.onJobStarted(jobName, parameters, this));
        }

        public void onFinish(JobOutcome result) {

            // invoke backwards - last callbacks are notified first
            ListIterator<Consumer<JobOutcome>> it = callbacks.listIterator(callbacks.size());
            while (it.hasPrevious()) {
                it.previous().accept(result);
            }
        }
    }
}
