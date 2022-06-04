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
import io.bootique.ConfigModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.job.JobRegistry;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;

import javax.inject.Singleton;

import static io.bootique.job.JobModule.LOG_LISTENER_ORDER;

public class JobInstrumentedModule extends ConfigModule {

    /**
     * @deprecated since 3.0 as InstrumentedJobListener is no longer implemented as a listener
     */
    @Deprecated
    public static final int JOB_LISTENER_ORDER = LOG_LISTENER_ORDER + 200;

    @Override
    public void configure(Binder binder) {
        binder.override(JobRegistry.class).toProvider(InstrumentedJobRegistryProvider.class);
    }

    @Provides
    @Singleton
    JobMetricsManager provideJobMetricsManager(MetricRegistry metricRegistry) {
        return new JobMetricsManager(metricRegistry);
    }

    @Provides
    @Singleton
    JobMDCManager provideJobMDCManager(TransactionIdGenerator generator, TransactionIdMDC mdc) {
        return new JobMDCManager(generator, mdc);
    }
}
