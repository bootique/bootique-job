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

package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.MappedJobListener;
import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

class JobRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunner.class);

    static JobResult run(Job job, Map<String, Object> parameters, Collection<MappedJobListener> listeners) {
        return listeners.isEmpty() ? runNoListeners(job, parameters) : runWithListeners(job, parameters, listeners);
    }

    private static JobResult runWithListeners(Job job, Map<String, Object> parameters, Collection<MappedJobListener> listeners) {

        String jobName = job.getMetadata().getName();
        JobListenerInvoker listenerInvoker = new JobListenerInvoker(jobName);
        listenerInvoker.onStart(listeners, parameters);

        JobResult result = runNoListeners(job, parameters);

        listenerInvoker.onFinish(result);
        return result;
    }

    private static JobResult runNoListeners(Job job, Map<String, Object> parameters) {
        try {
            JobResult result = job.run(parameters);
            return result != null ? result : JobResult.unknown(job.getMetadata());
        } catch (Exception e) {
            LOGGER.error(job.getMetadata().getName() + " failed to run.", e);
            return JobResult.failure(job.getMetadata(), e);
        }
    }

    static class JobListenerInvoker implements Consumer<Consumer<JobResult>> {

        private final String jobName;
        private final List<Consumer<JobResult>> callbacks;

        JobListenerInvoker(String jobName) {
            this.jobName = jobName;
            this.callbacks = new ArrayList<>(2);
        }

        @Override
        public void accept(Consumer<JobResult> callback) {
            callbacks.add(callback);
        }

        public void onStart(Collection<MappedJobListener> listeners, Map<String, Object> parameters) {
            listeners.stream().map(MappedJobListener::getListener).forEach(l -> {
                try {
                    l.onJobStarted(jobName, parameters, this);
                } catch (Exception e) {
                    LOGGER.error("Error invoking job listener for job: " + jobName, e);
                }
            });
        }

        public void onFinish(JobResult result) {

            // invoke backwards - last callbacks are notified first
            ListIterator<Consumer<JobResult>> it = callbacks.listIterator(callbacks.size());
            while (it.hasPrevious()) {
                try {
                    it.previous().accept(result);
                } catch (Exception e) {
                    LOGGER.error("Error invoking completion callback for job: " + jobName, e);
                }
            }
        }
    }
}