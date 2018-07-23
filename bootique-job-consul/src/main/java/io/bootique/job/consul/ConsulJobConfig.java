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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

/**
 * @since 0.26
 */
@BQConfig("Consul jobs configuration")
public class ConsulJobConfig {

    private String consulHost;
    private int consulPort;
    private String dataCenter;
    private String serviceGroup;

    public ConsulJobConfig() {
        this.consulHost = "localhost";
        this.consulPort = 8500;
    }

    @BQConfigProperty
    public void setConsulHost(String consulHost) {
        this.consulHost = consulHost;
    }

    @BQConfigProperty
    public void setConsulPort(int consulPort) {
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

    public String getConsulHost() {
        return consulHost;
    }

    public int getConsulPort() {
        return consulPort;
    }

    public String getDataCenter() {
        return dataCenter;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }
}
