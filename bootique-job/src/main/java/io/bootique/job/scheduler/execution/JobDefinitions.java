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
import io.bootique.job.config.JobDefinition;
import io.bootique.job.config.SingleJobDefinition;

import java.util.*;

class JobDefinitions {

    private final Map<String, ? extends JobDefinition> definitions;
    private final JobDefinitions defaults;

    JobDefinitions(Map<String, ? extends JobDefinition> definitions) {
        this(definitions, null);
    }

    JobDefinitions(Map<String, ? extends JobDefinition> definitions, JobDefinitions defaults) {
        this.definitions = Objects.requireNonNull(definitions);
        this.defaults = defaults;
    }

    JobDefinition getDefinition(String jobName) {
        JobDefinition definition = definitions.get(jobName);
        JobDefinition defaultDefinition = defaults != null ? defaults.getDefinition(jobName) : null;

        if (definition instanceof SingleJobDefinition && defaultDefinition instanceof SingleJobDefinition) {
            return mergeDefinitions((SingleJobDefinition) definition, (SingleJobDefinition) defaultDefinition);
        }

        if (definition == null && defaultDefinition == null) {
            throw new BootiqueException(1, "No job object for name '" + jobName + "'");
        }

        return definition != null ? definition : defaultDefinition;
    }

    private SingleJobDefinition mergeDefinitions(SingleJobDefinition overriding, SingleJobDefinition overriden) {
        return new SingleJobDefinition(mergeParams(overriding, overriden), mergeDependencies(overriding, overriden));
    }

    private Map<String, String> mergeParams(SingleJobDefinition overriding, SingleJobDefinition overriden) {
        Map<String, String> params = new HashMap<>(overriden.getParams());
        params.putAll(overriding.getParams());
        return params;
    }

    private Optional<List<String>> mergeDependencies(SingleJobDefinition overriding, SingleJobDefinition overriden) {
        return overriding.getDependsOn().isPresent() ? overriding.getDependsOn() : overriden.getDependsOn();
    }
}
