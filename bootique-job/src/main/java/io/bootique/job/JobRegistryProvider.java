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

import io.bootique.config.ConfigurationFactory;
import io.bootique.job.graph.JobGraphNode;
import io.bootique.job.graph.JobGraphNodeFactory;
import io.bootique.job.runnable.JobDecorators;
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
    protected final JobDecorators decorators;
    protected final Provider<Scheduler> scheduler;
    protected final ConfigurationFactory configFactory;

    @Inject
    public JobRegistryProvider(
            Set<Job> standaloneJobs,
            JobDecorators decorators,
            Provider<Scheduler> scheduler,
            ConfigurationFactory configFactory) {

        this.standaloneJobs = standaloneJobs;
        this.decorators = decorators;
        this.scheduler = scheduler;
        this.configFactory = configFactory;
    }

    @Override
    public JobRegistry get() {

        return new DefaultJobRegistry(
                standaloneJobs,
                graphNodes(),
                scheduler,
                decorators);
    }

    protected Map<String, JobGraphNode> graphNodes() {

        Map<String, JobGraphNode> nodes = new HashMap<>();

        TypeRef<Map<String, JobGraphNodeFactory>> ref = new TypeRef<>() {
        };
        configFactory.config(ref, JobModule.JOBS_CONFIG_PREFIX).forEach((k, v) -> nodes.put(k, v.create()));

        return nodes;
    }
}
