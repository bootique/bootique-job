package io.bootique.job.zookeeper;

import io.bootique.ConfigModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.job.JobModule;
import io.bootique.job.zookeeper.lock.ZkClusterLockHandler;
import org.apache.curator.framework.CuratorFramework;

import javax.inject.Provider;
import javax.inject.Singleton;

public class ZkJobModule extends ConfigModule {

    @Override
    protected String defaultConfigPrefix() {
        return "job-zookeeper";
    }

    @Override
    public void configure(Binder binder) {
        JobModule.extend(binder).addLockHandler(ZkClusterLockHandler.class);
    }

    @Provides
    @Singleton
    ZkClusterLockHandler provideZkLockHandler(Provider<CuratorFramework> curatorFramework) {
        return new ZkClusterLockHandler(curatorFramework);
    }
}
