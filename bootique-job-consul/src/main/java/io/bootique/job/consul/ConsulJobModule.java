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

import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.job.consul.lock.CompositeConsulLockHandler;
import io.bootique.job.runtime.JobModule;
import io.bootique.shutdown.ShutdownManager;

import javax.inject.Singleton;


public class ConsulJobModule extends ConfigModule {

    @Override
    protected String defaultConfigPrefix() {
        return "job-consul";
    }

    @Override
    public void configure(Binder binder) {
        JobModule.extend(binder).addLockHandler("consul", CompositeConsulLockHandler.class);
    }

    @Provides
    @Singleton
    public CompositeConsulLockHandler provideConsulLockHandler(
            ConfigurationFactory configFactory,
            ShutdownManager shutdownManager) {
        return config(ConsulLockHandlerFactory.class, configFactory).createLockHandler(shutdownManager);
    }
}
