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

import io.bootique.annotation.BQConfig;
import io.bootique.job.JobRegistry;
import io.bootique.job.runtime.GraphExecutor;
import io.bootique.job.runtime.JobDecorators;
import io.bootique.job.scheduler.SchedulerFactory;
import io.bootique.shutdown.ShutdownManager;

import jakarta.inject.Inject;

/**
 * @since 3.0
 */
@BQConfig
public class InstrumentedSchedulerFactory extends SchedulerFactory {

    @Inject
    public InstrumentedSchedulerFactory(
            JobRegistry jobRegistry,
            JobDecorators decorators,
            ShutdownManager shutdownManager) {
        super(jobRegistry, decorators, shutdownManager);
    }

    @Override
    public GraphExecutor createGraphExecutor() {
        return new InstrumentedGraphExecutor(createGraphExecutorService());
    }
}
