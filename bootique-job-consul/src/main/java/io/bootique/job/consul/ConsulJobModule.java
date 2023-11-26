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
package io.bootique.job.consul;

import io.bootique.BQModuleProvider;
import io.bootique.ModuleCrate;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.job.JobModule;
import io.bootique.job.consul.lock.CompositeConsulLockHandler;
import io.bootique.shutdown.ShutdownManager;

import javax.inject.Singleton;


public class ConsulJobModule implements BQModule, BQModuleProvider {

    // TODO: "-" is not following BQ naming convention
    private static final String CONFIG_PREFIX = "job-consul";

    @Override
    public ModuleCrate moduleCrate() {
        return ModuleCrate.of(this)
                .description("Integrates Consul-based Bootique job locks")
                .config(CONFIG_PREFIX, ConsulLockHandlerFactory.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {
        JobModule.extend(binder).setLockHandler(CompositeConsulLockHandler.class);
    }

    @Provides
    @Singleton
    public CompositeConsulLockHandler provideConsulLockHandler(
            ConfigurationFactory configFactory,
            ShutdownManager shutdownManager) {
        return configFactory
                .config(ConsulLockHandlerFactory.class, CONFIG_PREFIX)
                .createLockHandler(shutdownManager);
    }
}
