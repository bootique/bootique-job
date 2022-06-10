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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @since 3.0
 */
@BQConfig("Job group. Aggregates a number of jobs and job groups, that should be run together, possibly depending on each other." +
        " Provides the means to alter the \"default\" job configuration" +
        " (override parameters, that were specified in the job definition; add new parameters;" +
        " and also override the list of job's dependencies).")
@JsonTypeName("group")
public class GroupNodeFactory implements JobGraphNodeFactory<GroupNode> {

    // TODO: we should be able to use groups as children, but this requires testing of the entire dispatch mechanism.
    //   E.g. JobGraphBuilder has some SingleJobDefinition casts
    private Map<String, SingleJobNodeFactory> jobs;

    @Override
    public GroupNode create() {

        if (jobs == null) {
            return new GroupNode(Collections.emptyMap());
        }

        Map<String, SingleJobNode> children = new HashMap<>();

        for (Map.Entry<String, SingleJobNodeFactory> j : jobs.entrySet()) {
            Objects.requireNonNull(j.getValue(), () -> "Null job definition for group job '" + j.getKey() + "'");
            children.put(j.getKey(), j.getValue().create());
        }

        return new GroupNode(children);
    }

    @BQConfigProperty("Jobs and job groups, that belong to this job group." +
            " Overriding of default parameters and dependencies can be done here.")
    public void setJobs(Map<String, SingleJobNodeFactory> jobs) {
        this.jobs = jobs;
    }
}
