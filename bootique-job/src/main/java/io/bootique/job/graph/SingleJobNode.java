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

import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 3.0
 */
public class SingleJobNode implements JobGraphNode {

    private final Map<String, String> params;
    private final List<String> dependsOn;
    private final boolean forceNoDependencies;

    public SingleJobNode() {
        this(Collections.emptyMap(), Collections.emptyList(), false);
    }

    public SingleJobNode(Map<String, String> params, List<String> dependsOn, boolean forceNoDependencies) {
        this.params = Objects.requireNonNull(params);
        this.dependsOn = Objects.requireNonNull(dependsOn);
        this.forceNoDependencies = forceNoDependencies;

        if (forceNoDependencies && !dependsOn.isEmpty()) {
            throw new IllegalStateException("GraphNode is forcing no dependencies, yet the dependencies are provided");
        }
    }

    @Override
    public void accept(JobGraphNodeVisitor v) {
        v.visitSingle(this);
    }

    public SingleJobNode merge(SingleJobNode overriding) {

        Map<String, String> mergedParams = new HashMap<>(params);
        mergedParams.putAll(overriding.getParams());

        List<String> mergedDependsOn = overriding.forceNoDependencies || !overriding.dependsOn.isEmpty()
                ? overriding.dependsOn
                : this.dependsOn;

        return new SingleJobNode(mergedParams, mergedDependsOn, false);
    }

    public Map<String, String> getParams() {
        return params;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    @Override
    public String toString() {
        return "job => depends:"
                + dependsOn.stream().collect(Collectors.joining(",", "[", "]"))
                + ", params: " + params.keySet().stream().collect(Collectors.joining(",", "[", "]"));
    }
}
