/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.MappedJobListener;
import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

class Callback implements Consumer<Consumer<JobResult>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Callback.class);

    static JobResult runAndNotify(Job job, Map<String, Object> parameters, Collection<MappedJobListener> listeners) {
        String jobName = job.getMetadata().getName();
        Optional<Callback> callbackOptional = listeners.isEmpty() ? Optional.empty() : Optional.of(new Callback(jobName));

        callbackOptional.ifPresent(callback -> listeners.forEach(listener -> {
            try {
                listener.getListener().onJobStarted(jobName, parameters, callback);
            } catch (Exception e) {
                LOGGER.error("Error invoking job listener for job: " + jobName, e);
            }
        }));

        JobResult result;
        try {
            result = job.run(parameters);
        } catch (Exception e) {
            result = JobResult.failure(job.getMetadata(), e);
            if (callbackOptional.isPresent()) {
                callbackOptional.get().invoke(result);
            }
        }
        if (result == null) {
            result = JobResult.unknown(job.getMetadata());
        }
        if (callbackOptional.isPresent()) {
            callbackOptional.get().invoke(result);
        }
        return result;
    }

    private String jobName;
    private LinkedList<Consumer<JobResult>> callbacks;

    public Callback(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public void accept(Consumer<JobResult> callback) {
        if (callbacks == null) {
            callbacks = new LinkedList<>();
        }
        callbacks.add(callback);
    }

    public void invoke(JobResult result) {
        Iterator<Consumer<JobResult>> itr = callbacks.descendingIterator();
        while (itr.hasNext()) {
            try {
                itr.next().accept(result);
            } catch (Exception e) {
                LOGGER.error("Error invoking completion callback for job: " + jobName, e);
            }
        }
    }
}