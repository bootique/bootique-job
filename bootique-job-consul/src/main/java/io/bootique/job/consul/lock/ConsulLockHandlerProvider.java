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
package io.bootique.job.consul.lock;

import com.google.common.net.HostAndPort;
import com.google.inject.Provider;
import com.orbitz.consul.Consul;
import io.bootique.job.consul.ConsulSession;
import io.bootique.job.lock.LockHandler;
import io.bootique.shutdown.ShutdownManager;

/**
 * @since 1.0.RC1
 */
public class ConsulLockHandlerProvider implements Provider<LockHandler> {

    private final String host;
    private final int port;
    private final String dataCenter;
    private final String serviceGroup;
    private final ShutdownManager shutdownManager;

    public ConsulLockHandlerProvider(String host, int port, String dataCenter,
                                     String serviceGroup, ShutdownManager shutdownManager) {
        this.host = host;
        this.port = port;
        this.dataCenter = dataCenter;
        this.serviceGroup = serviceGroup;
        this.shutdownManager = shutdownManager;
    }

    @Override
    public LockHandler get() {
        HostAndPort hostAndPort = HostAndPort.fromParts(host, port);
        Consul consul = Consul.builder().withHostAndPort(hostAndPort).build();
        ConsulSession session = new ConsulSession(consul.sessionClient(), dataCenter);
        shutdownManager.addShutdownHook(session::destroySessionIfPresent);
        return new ConsulLockHandler(consul.keyValueClient(), session::getOrCreateSession, serviceGroup);
    }
}
