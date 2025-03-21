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

import com.codahale.metrics.MetricRegistry;
import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.job.JobsModule;
import io.bootique.job.SchedulerModule;
import io.bootique.job.runtime.GraphExecutor;
import io.bootique.job.runtime.JobLogger;
import io.bootique.metrics.mdc.TransactionIdGenerator;

import jakarta.inject.Singleton;

public class JobInstrumentedModule implements BQModule {

    // same prefix as the module we instrument
    private static final String SCHEDULER_CONFIG_PREFIX = "scheduler";

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Integrates metrics and extra logging in the Bootique job engine")
                .overrides(JobsModule.class, SchedulerModule.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {
    }

    @Provides
    @Singleton
    GraphExecutor createGraphExecutor(ConfigurationFactory configFactory) {
        return configFactory.config(InstrumentedSchedulerFactory.class, SCHEDULER_CONFIG_PREFIX).createGraphExecutor();
    }

    @Provides
    @Singleton
    JobMetricsManager provideJobMetricsManager(MetricRegistry metricRegistry) {
        return new JobMetricsManager(metricRegistry);
    }

    @Provides
    @Singleton
    JobMDCManager provideJobMDCManager(TransactionIdGenerator generator) {
        return new JobMDCManager(generator);
    }

    @Provides
    @Singleton
    JobLogger provideJobLogger(JobMDCManager mdcManager, JobMetricsManager metricsManager) {
        return new InstrumentedJobLogger(mdcManager, metricsManager);
    }
}
