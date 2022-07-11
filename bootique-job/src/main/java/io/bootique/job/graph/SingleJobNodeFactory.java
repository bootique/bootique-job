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

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * @since 3.0
 */
@BQConfig("Standalone job with optional dependencies.")
@JsonTypeName("single")
public class SingleJobNodeFactory implements JobGraphNodeFactory<SingleJobNode> {

    private Map<String, String> params;
    private List<String> dependsOn;

    @Override
    public SingleJobNode create() {
        return new SingleJobNode(
                params != null ? params : Collections.emptyMap(),
                dependsOn != null ? new LinkedHashSet<>(dependsOn) : Collections.emptySet(),

                // an explicitly set empty list means that, when the node is used as an override, overridden
                // node's dependencies will need to be wiped out
                dependsOn != null && dependsOn.isEmpty()
        );
    }

    @BQConfigProperty
    public void setParams(Map<String, String> params) {
        this.params = params;
    }


    @BQConfigProperty("List of dependencies, that should be run prior to the current job." +
            " May include names of both standalone jobs and job groups." +
            " The order of execution of dependencies may be different from the order, in which they appear in this list." +
            " If you'd like the dependencies to be executed in a particular order, consider creating an explicit job group.")
    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }
}
