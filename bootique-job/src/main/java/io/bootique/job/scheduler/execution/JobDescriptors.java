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
import io.bootique.job.descriptor.JobDescriptor;
import io.bootique.job.descriptor.SingleJobDescriptor;

import java.util.*;

class JobDescriptors {

    private final Map<String, ? extends JobDescriptor> descriptors;
    private final JobDescriptors defaults;

    JobDescriptors(Map<String, ? extends JobDescriptor> descriptors) {
        this(descriptors, null);
    }

    JobDescriptors(Map<String, ? extends JobDescriptor> descriptors, JobDescriptors defaults) {
        this.descriptors = Objects.requireNonNull(descriptors);
        this.defaults = defaults;
    }

    JobDescriptor getDescriptor(String jobName) {
        JobDescriptor descriptor = descriptors.get(jobName);
        JobDescriptor defaultDescriptor = defaults != null ? defaults.getDescriptor(jobName) : null;

        if (descriptor instanceof SingleJobDescriptor && defaultDescriptor instanceof SingleJobDescriptor) {
            return ((SingleJobDescriptor) defaultDescriptor).merge((SingleJobDescriptor) descriptor);
        }

        if (descriptor == null && defaultDescriptor == null) {
            throw new BootiqueException(1, "No job object for name '" + jobName + "'");
        }

        return descriptor != null ? descriptor : defaultDescriptor;
    }
}
