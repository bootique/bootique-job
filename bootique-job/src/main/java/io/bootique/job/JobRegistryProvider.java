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
import io.bootique.job.graph.JobNode;
import io.bootique.job.runtime.DefaultJobRegistry;
import io.bootique.job.runtime.GraphExecutor;
import io.bootique.job.runtime.JobDecorators;
import io.bootique.type.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

/**
 * @since 3.0
 */
public class JobRegistryProvider implements Provider<JobRegistry> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRegistryProvider.class);

    protected final Set<Job> standaloneJobs;
    protected final JobDecorators decorators;
    protected final Provider<GraphExecutor> graphExecutor;
    protected final ConfigurationFactory configFactory;

    @Inject
    public JobRegistryProvider(
            Set<Job> standaloneJobs,
            JobDecorators decorators,
            Provider<GraphExecutor> graphExecutor,
            ConfigurationFactory configFactory) {

        this.standaloneJobs = standaloneJobs;
        this.decorators = decorators;
        this.graphExecutor = graphExecutor;
        this.configFactory = configFactory;
    }

    @Override
    public JobRegistry get() {

        Map<String, Job> jobsByName = jobsByName(standaloneJobs);

        return new DefaultJobRegistry(
                graphNodes(jobsByName),
                decorators,
                graphExecutor);
    }

    protected Map<String, Job> jobsByName(Collection<Job> jobs) {

        // report job name conflicts, but otherwise ignore them
        // TODO: should we throw?

        Map<String, Job> map = new HashMap<>();
        for (Job j : jobs) {

            String name = j.getMetadata().getName();
            Job existing = map.put(name, j);
            if (existing != null && existing != j) {
                LOGGER.warn("Duplicate job name '{}' was ignored and one of the jobs discarded", name);
            }
        }

        return map;
    }

    protected Map<String, JobGraphNode> graphNodes(Map<String, Job> jobsByName) {

        Map<String, JobGraphNode> nodes = new HashMap<>();

        TypeRef<Map<String, JobGraphNodeFactory>> ref = new TypeRef<>() {
        };
        configFactory.config(ref, JobModule.JOBS_CONFIG_PREFIX).forEach((k, v) -> nodes.put(k, v.create(k, jobsByName)));

        // now create "implicit" node descriptors for those jobs that are present as Java classes, but don't have
        // explicit overriding configs
        jobsByName.entrySet()
                .stream()
                .filter(e -> !nodes.containsKey(e.getKey()))
                .forEach(e -> nodes.put(e.getKey(), createDefaultNode(e.getValue())));

        return nodes;
    }

    protected JobGraphNode createDefaultNode(Job job) {
        JobMetadata metadata = job.getMetadata();
        return new JobNode(job, createDefaultParams(metadata), metadata.getDependsOn(), false);
    }

    protected Map<String, Object> createDefaultParams(JobMetadata metadata) {

        if (metadata.getParameters().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> map = new HashMap<>();
        metadata.getParameters().forEach(p -> map.put(p.getName(), p.getDefaultValue()));
        return map;
    }
}
