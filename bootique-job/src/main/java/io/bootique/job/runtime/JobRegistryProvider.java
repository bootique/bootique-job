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

import io.bootique.config.ConfigurationFactory;
import io.bootique.job.Job;
import io.bootique.job.JobListener;
import io.bootique.job.JobRegistry;
import io.bootique.job.MappedJobListener;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.scheduler.execution.DefaultJobRegistry;
import io.bootique.type.TypeRef;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

/**
 * @since 3.0
 */
public class JobRegistryProvider implements Provider<JobRegistry> {

    protected final Set<Job> standaloneJobs;
    protected final Set<JobListener> listeners;
    protected final Set<MappedJobListener> mappedListeners;
    protected final Provider<Scheduler> scheduler;
    protected final ConfigurationFactory configFactory;

    @Inject
    public JobRegistryProvider(
            Set<Job> standaloneJobs,
            Set<JobListener> listeners,
            Set<MappedJobListener> mappedListeners,
            Provider<Scheduler> scheduler,
            ConfigurationFactory configFactory) {

        this.standaloneJobs = standaloneJobs;
        this.listeners = listeners;
        this.mappedListeners = mappedListeners;
        this.scheduler = scheduler;
        this.configFactory = configFactory;
    }

    @Override
    public JobRegistry get() {
        return new DefaultJobRegistry(
                standaloneJobs,
                jobConfigs(),
                scheduler,
                combineListeners());
    }

    protected Map<String, JobDefinition> jobConfigs() {
        TypeRef<Map<String, JobDefinition>> ref = new TypeRef<>() {
        };
        return configFactory.config(ref, JobModule.JOBS_CONFIG_PREFIX);
    }

    protected List<MappedJobListener> combineListeners() {

        // not checking for dupes between MappedJobListener and JobListener collections. Is that a problem?
        List<MappedJobListener> localListeners = new ArrayList<>(mappedListeners.size() + listeners.size());
        localListeners.addAll(mappedListeners);

        //  Integer.MAX_VALUE means placing bare unordered listeners after (== inside) mapped listeners
        listeners.forEach(listener -> localListeners.add(new MappedJobListener<>(listener, Integer.MAX_VALUE)));
        localListeners.sort(Comparator.comparing(MappedJobListener::getOrder));

        return localListeners;
    }
}
