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
import io.bootique.job.JobMetadata;
import io.bootique.job.MappedJobListener;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;

import java.util.Collection;
import java.util.Map;

class JobGroup implements Job {

    private volatile ExecutableJobGroup delegate;

    private final String name;
    private final Collection<Job> jobs;
    private final DependencyGraph graph;
    private final Scheduler scheduler;
    private final Collection<MappedJobListener> listeners;

    public JobGroup(
            String name,
            Collection<Job> jobs,
            DependencyGraph graph,
            Scheduler scheduler,
            Collection<MappedJobListener> listeners) {

        this.name = name;
        this.jobs = jobs;

        this.graph = graph;
        this.scheduler = scheduler;
        this.listeners = listeners;
    }

    private ExecutableJobGroup getDelegate() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = ExecutableJobGroup.create(name, scheduler, graph, jobs);
                }
            }
        }
        return delegate;
    }

    @Override
    public JobMetadata getMetadata() {
        return getDelegate().getMetadata();
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        // TODO: merge execution params into individual jobs' params
        return JobRunner.run(getDelegate(), params, listeners);
    }
}
