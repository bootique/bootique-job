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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class Environment {

    private Map<String, ? extends JobDefinition> definitions;
    private Optional<Environment> defaultEnvironment;

    public Environment(Map<String, ? extends JobDefinition> definitions) {
        this(definitions, null);
    }

    public Environment(Map<String, ? extends JobDefinition> definitions,
                       Environment defaultEnvironment) {
        this.definitions = definitions;
        this.defaultEnvironment = Optional.ofNullable(defaultEnvironment);
    }

    public JobDefinition getDefinition(String jobName) {
        JobDefinition definition = definitions.get(jobName);
        if (defaultEnvironment.isPresent()) {
            if (definition == null) {
                definition = defaultEnvironment.get().getDefinition(jobName);
            } else if (definition instanceof SingleJobDefinition) {
                JobDefinition delegateDefinition = defaultEnvironment.get().getDefinition(jobName);
                if (delegateDefinition instanceof SingleJobDefinition) {
                    definition = mergeDefinitions((SingleJobDefinition) definition, (SingleJobDefinition) delegateDefinition);
                }
            }
        }

        if(definition == null) {
            throw new BootiqueException(1, "No job object for name '" + jobName + "'");
        }

        return definition;
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
