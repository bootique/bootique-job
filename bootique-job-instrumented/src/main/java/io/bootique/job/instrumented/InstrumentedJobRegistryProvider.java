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
package io.bootique.job.instrumented;

import io.bootique.config.ConfigurationFactory;
import io.bootique.job.*;
import io.bootique.job.runnable.JobDecorators;
import io.bootique.job.scheduler.Scheduler;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

/**
 * @since 3.0
 */
public class InstrumentedJobRegistryProvider extends JobRegistryProvider {

    private final JobMDCManager mdcManager;

    @Inject
    public InstrumentedJobRegistryProvider(
            Set<Job> standaloneJobs,
            JobDecorators decorators,
            Provider<Scheduler> scheduler,
            JobMDCManager mdcManager,
            ConfigurationFactory configFactory) {
        super(standaloneJobs, decorators, scheduler, configFactory);
        this.mdcManager = mdcManager;
    }

    @Override
    public JobRegistry get() {
        return new InstrumentedJobRegistry(
                standaloneJobs,
                graphNodes(),
                scheduler,
                decorators,
                mdcManager);
    }
}
