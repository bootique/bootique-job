package io.bootique.job.consul.lock;

import com.google.common.net.HostAndPort;
import com.google.inject.Provider;
import com.orbitz.consul.Consul;
import io.bootique.job.consul.ConsulSession;
import io.bootique.job.lock.LockHandler;
import io.bootique.shutdown.ShutdownManager;

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
