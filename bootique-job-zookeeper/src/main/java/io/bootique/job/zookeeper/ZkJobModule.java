package io.bootique.job.zookeeper;

import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.ConfigModule;
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
        return "job-consul";
    }

    @Provides
    @Singleton
    LockHandler provideClusteredLockHandler(Injector injector) {
        return new ZkClusterLockHandler(injector);
    }
}
