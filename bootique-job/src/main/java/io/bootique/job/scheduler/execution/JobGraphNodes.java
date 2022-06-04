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

import io.bootique.BootiqueException;
import io.bootique.job.graph.JobGraphNode;
import io.bootique.job.graph.SingleJobNode;

import java.util.*;

class JobGraphNodes {

    private final Map<String, ? extends JobGraphNode> nodes;
    private final JobGraphNodes defaults;

    JobGraphNodes(Map<String, ? extends JobGraphNode> nodes) {
        this(nodes, null);
    }

    JobGraphNodes(Map<String, ? extends JobGraphNode> nodes, JobGraphNodes defaults) {
        this.nodes = Objects.requireNonNull(nodes);
        this.defaults = defaults;
    }

    JobGraphNode getNode(String jobName) {
        JobGraphNode defaultNode = defaults != null ? defaults.getNode(jobName) : null;
        JobGraphNode overrideNode = nodes.get(jobName);

        if (overrideNode instanceof SingleJobNode && defaultNode instanceof SingleJobNode) {
            return ((SingleJobNode) defaultNode).merge((SingleJobNode) overrideNode);
        }

        if (overrideNode == null && defaultNode == null) {
            throw new BootiqueException(1, "No job object for name '" + jobName + "'");
        }

        return overrideNode != null ? overrideNode : defaultNode;
    }
}
