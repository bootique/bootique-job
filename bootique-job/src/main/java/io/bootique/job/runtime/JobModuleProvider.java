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

package io.bootique.job.runtime;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.scheduler.SchedulerFactory;
import io.bootique.type.TypeRef;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JobModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new JobModule();
    }

    @Override
    public Map<String, Type> configs() {

        TypeRef<Map<String,JobDefinition>> jobs = new TypeRef<Map<String,JobDefinition>>() {};

        Map<String, Type> configs = new HashMap<>();
        configs.put("scheduler", SchedulerFactory.class);
        configs.put("jobs", jobs.getType());
        return configs;
    }
}
