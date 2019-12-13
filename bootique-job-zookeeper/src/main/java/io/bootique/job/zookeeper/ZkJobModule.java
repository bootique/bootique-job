package io.bootique.job.zookeeper;

import javax.inject.Singleton;

import io.bootique.ConfigModule;
import io.bootique.di.Injector;
import io.bootique.di.Provides;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.zookeeper.lock.ZkClusterLockHandler;

@SuppressWarnings("unused")
public class ZkJobModule extends ConfigModule {

    public ZkJobModule() {
    }

    public ZkJobModule(String configPrefix) {
        super(configPrefix);
    }

    @Override
    protected String defaultConfigPrefix() {
        return "job-zookeeper";
    }

    @Provides
    @Singleton
    LockHandler provideClusteredLockHandler(Injector injector) {
        return new ZkClusterLockHandler(injector);
    }
}
