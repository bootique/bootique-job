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

package io.bootique.job.graph;

import io.bootique.job.Job;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A job dependency graph node representing a specific job.
 *
 * @since 3.0
 */
public class JobNode implements JobGraphNode {

    private final Job job;
    private final Map<String, Object> params;
    private final Set<String> dependsOn;
    private final boolean forceNoDependencies;

    public JobNode(Job job, Map<String, Object> params, Set<String> dependsOn, boolean forceNoDependencies) {
        this.job = job;
        this.params = Objects.requireNonNull(params);
        this.dependsOn = Objects.requireNonNull(dependsOn);

        // Helps to distinguish between empty dependencies and no dependencies. Used in the node merge logic
        this.forceNoDependencies = forceNoDependencies;

        if (forceNoDependencies && !dependsOn.isEmpty()) {
            throw new IllegalStateException("GraphNode is forcing no dependencies, yet the dependencies are provided");
        }
    }

    @Override
    public void accept(JobGraphNodeVisitor v) {
        v.visitJob(this);
    }

    @Override
    public String getName() {
        return job.getMetadata().getName();
    }

    @Override
    public Set<String> getDependsOn() {
        return dependsOn;
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    public Job getJob() {
        return job;
    }

    public JobNode merge(JobNode overriding) {

        Map<String, Object> mergedParams = new HashMap<>(params);
        mergedParams.putAll(overriding.getParams());

        Set<String> mergedDependsOn = overriding.forceNoDependencies || !overriding.dependsOn.isEmpty()
                ? overriding.dependsOn
                : this.dependsOn;

        return new JobNode(overriding.job, mergedParams, mergedDependsOn, false);
    }

    @Override
    public String toString() {
        return "job '" + getName() + "' => depends:"
                + dependsOn.stream().collect(Collectors.joining(",", "[", "]"))
                + ", params: " + params.keySet().stream().collect(Collectors.joining(",", "[", "]"));
    }
}
