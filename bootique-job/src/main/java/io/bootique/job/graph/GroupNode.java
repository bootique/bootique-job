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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A job dependency graph node representing a group of jobs.
 *
 * @since 3.0
 */
public class GroupNode implements JobGraphNode {

    private final String name;
    private final Map<String, JobNode> children;

    public GroupNode(String name, Map<String, JobNode> children) {
        this.name = name;
        this.children = children;
    }

    @Override
    public void accept(JobGraphNodeVisitor v) {
        v.visitGroup(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getDependsOn() {
        return children.keySet();
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    @Override
    public Map<String, Object> getParams() {
        return Collections.emptyMap();
    }

    public Map<String, JobNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return "job group '" + getName() + "' => jobs: " + getDependsOn().stream().collect(Collectors.joining(",", "[", "]"));
    }
}
