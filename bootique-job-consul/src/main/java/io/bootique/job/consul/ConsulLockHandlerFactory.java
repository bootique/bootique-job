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

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.job.consul.lock.CompositeConsulLockHandler;
import io.bootique.job.consul.lock.ConsulLockHandler;
import io.bootique.job.lock.LocalLockHandler;
import io.bootique.job.lock.LockHandler;
import io.bootique.shutdown.ShutdownManager;

/**
 * @since 3.0.M1
 */
@BQConfig("Consul jobs configuration")
public class ConsulLockHandlerFactory {

    private String consulHost;
    private Integer consulPort;
    private String dataCenter;
    private String serviceGroup;

    public ConsulLockHandlerFactory() {
        this.consulHost = "localhost";
        this.consulPort = 8500;
    }

    public CompositeConsulLockHandler createLockHandler(ShutdownManager shutdownManager) {

        String host = this.consulHost != null ? this.consulHost : "localhost";
        int port = this.consulPort != null ? this.consulPort : 8500;

        HostAndPort hostAndPort = HostAndPort.fromParts(host, port);
        Consul consul = Consul.builder().withHostAndPort(hostAndPort).build();
        ConsulSession session = new ConsulSession(consul.sessionClient(), dataCenter);
        shutdownManager.addShutdownHook(session::destroySessionIfPresent);
        LockHandler localLockHandler = new LocalLockHandler();
        LockHandler consulLockHandler = new ConsulLockHandler(
                consul.keyValueClient(),
                session::getOrCreateSession,
                serviceGroup
        );
        return new CompositeConsulLockHandler(localLockHandler, consulLockHandler);
    }

    @BQConfigProperty
    public void setConsulHost(String consulHost) {
        this.consulHost = consulHost;
    }

    @BQConfigProperty
    public void setConsulPort(Integer consulPort) {
        this.consulPort = consulPort;
    }

    @BQConfigProperty
    public void setDataCenter(String dataCenter) {
        this.dataCenter = dataCenter;
    }

    @BQConfigProperty("Value to prepend to job lock names to distinguish them from other services that use Consul")
    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }
}
