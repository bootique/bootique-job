/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.job.instrumented;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.ConfigModule;
import io.bootique.job.MappedJobListener;
import io.bootique.job.runtime.JobModule;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;

import static io.bootique.job.runtime.JobModule.BUSINESS_TX_LISTENER_ORDER;
import static io.bootique.job.runtime.JobModule.LOG_LISTENER_ORDER;

/**
 * @since 0.14
 */
public class JobInstrumentedModule extends ConfigModule {

    public static final int JOB_LISTENER_ORDER = LOG_LISTENER_ORDER + 200;

    public JobInstrumentedModule() {

    }

    public JobInstrumentedModule(String configPrefix) {
        super(configPrefix);
    }

    @Override
    public void configure(Binder binder) {
        JobModule.extend(binder)
                .addMappedListener(new TypeLiteral<MappedJobListener<InstrumentedJobListener>>() {
                })
                .addMappedListener(new TypeLiteral<MappedJobListener<JobMDCManager>>() {
                });
    }

    @Provides
    @Singleton
    public MappedJobListener<InstrumentedJobListener> provideInstrumentedJobListener(MetricRegistry metricRegistry) {
        return new MappedJobListener<>(new InstrumentedJobListener(metricRegistry), JOB_LISTENER_ORDER);
    }

    @Provides
    @Singleton
    MappedJobListener<JobMDCManager> provideJobMDCManager(TransactionIdGenerator generator, TransactionIdMDC mdc) {
        JobMDCManager mdcManager = new JobMDCManager(generator, mdc);
        return new MappedJobListener<>(mdcManager, BUSINESS_TX_LISTENER_ORDER);
    }
}
