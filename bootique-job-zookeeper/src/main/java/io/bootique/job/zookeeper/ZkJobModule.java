package io.bootique.job.zookeeper;

import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.job.JobsModule;
import io.bootique.job.zookeeper.lock.ZkClusterLockHandler;
import org.apache.curator.framework.CuratorFramework;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public class ZkJobModule implements BQModule {

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Integrates Zookeeper-based Bootique job locks")
                .build();
    }

    @Override
    public void configure(Binder binder) {
        JobsModule.extend(binder).setLockHandler(ZkClusterLockHandler.class);
    }

    @Provides
    @Singleton
    ZkClusterLockHandler provideZkLockHandler(Provider<CuratorFramework> curatorFramework) {
        return new ZkClusterLockHandler(curatorFramework);
    }
}
