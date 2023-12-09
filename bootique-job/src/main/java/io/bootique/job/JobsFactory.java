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

import com.fasterxml.jackson.annotation.JsonCreator;
import io.bootique.annotation.BQConfig;
import io.bootique.job.graph.JobGraphNode;
import io.bootique.job.graph.JobGraphNodeFactory;
import io.bootique.job.graph.JobNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * @since 3.0
 */
public class JobsFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsFactory.class);

    // TODO: constructor injection is taken over by Jackson; how do we mix @JsonCreator with BQ services?
    @Inject
    protected Set<Job> standaloneJobs;

    private final Map<String, JobGraphNodeFactory> jobs;

    @BQConfig("A map of jobs by name")
    @JsonCreator
    public JobsFactory(Map<String, JobGraphNodeFactory> jobs) {
        this.jobs = jobs;
    }

    public Map<String, JobGraphNode> create() {

        Objects.requireNonNull(standaloneJobs, "'standaloneJobs' was not injected");

        Map<String, Job> jobsByName = jobsByName(standaloneJobs);
        Map<String, JobGraphNode> nodes = new HashMap<>();

        getJobs().forEach((k, v) -> nodes.put(k, v.create(k, jobsByName)));

        // now create "implicit" node descriptors for those jobs that are present as Java classes, but don't have
        // explicit overriding configs
        jobsByName.entrySet()
                .stream()
                .filter(e -> !nodes.containsKey(e.getKey()))
                .forEach(e -> nodes.put(e.getKey(), createDefaultNode(e.getValue())));

        return nodes;
    }

    private Map<String, Job> jobsByName(Collection<Job> jobs) {

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

    private JobGraphNode createDefaultNode(Job job) {
        JobMetadata metadata = job.getMetadata();
        return new JobNode(job, createDefaultParams(metadata), metadata.getDependsOn(), false);
    }

    private Map<String, JobGraphNodeFactory> getJobs() {
        return jobs != null ? jobs : Collections.emptyMap();
    }

    private Map<String, Object> createDefaultParams(JobMetadata metadata) {

        if (metadata.getParameters().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> map = new HashMap<>();
        metadata.getParameters().forEach(p -> map.put(p.getName(), p.getDefaultValue()));
        return map;
    }
}
