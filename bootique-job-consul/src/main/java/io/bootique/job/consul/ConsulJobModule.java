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

import javax.inject.Singleton;

import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Key;
import io.bootique.di.Provides;
import io.bootique.job.consul.lock.ConsulLockHandlerProvider;
import io.bootique.job.lock.LockHandler;
import io.bootique.shutdown.ShutdownManager;

/**
 * @since 1.0.RC1
 */
public class ConsulJobModule extends ConfigModule {

    public ConsulJobModule() {
    }

    public ConsulJobModule(String configPrefix) {
        super(configPrefix);
    }

    @Override
    protected String defaultConfigPrefix() {
        return "job-consul";
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Key.get(LockHandler.class)).toProvider(ConsulLockHandlerProvider.class);
    }

    @Provides
    @Singleton
    public ConsulLockHandlerProvider provideConsulLockHandlerProvider(
            ConfigurationFactory configFactory, ShutdownManager shutdownManager) {

        ConsulJobConfig config = config(ConsulJobConfig.class, configFactory);

        return new ConsulLockHandlerProvider(
                config.getConsulHost(),
                config.getConsulPort(),
                config.getDataCenter(),
                config.getServiceGroup(),
                shutdownManager);
    }
}
