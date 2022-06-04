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

package io.bootique.job.descriptor;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 3.0
 */
public class JobGroupDescriptor implements JobDescriptor {

    private final Map<String, SingleJobDescriptor> jobs;

    public JobGroupDescriptor(Map<String, SingleJobDescriptor> jobs) {
        this.jobs = jobs;
    }

    @Override
    public void accept(JobDescriptorVisitor v) {
        v.visitGroup(this);
    }

    public Map<String, SingleJobDescriptor> getJobs() {
        return jobs;
    }

    @Override
    public String toString() {
        return "job group => jobs: " + jobs.keySet().stream().collect(Collectors.joining(",", "[", "]"));
    }
}
