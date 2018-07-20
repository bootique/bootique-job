package io.bootique.job.consul;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

@BQConfig("Consul jobs configuration")
public class ConsulJobConfig {

    private String consulHost;
    private Integer consulPort;
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

    public String getConsulHost() {
        return consulHost;
    }

    public Integer getConsulPort() {
        return consulPort;
    }

    public String getDataCenter() {
        return dataCenter;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }
}
