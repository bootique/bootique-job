package io.bootique.job.zookeeper;

import io.bootique.BQModuleProvider;
import io.bootique.ModuleCrate;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.job.JobModule;
import io.bootique.job.zookeeper.lock.ZkClusterLockHandler;
import org.apache.curator.framework.CuratorFramework;

import javax.inject.Provider;
import javax.inject.Singleton;

public class ZkJobModule implements BQModule, BQModuleProvider {

    @Override
    public ModuleCrate moduleCrate() {
        return ModuleCrate.of(this)
                .description("Integrates Zookeeper-based Bootique job locks")
                .build();
    }

    @Override
    public void configure(Binder binder) {
        JobModule.extend(binder).setLockHandler(ZkClusterLockHandler.class);
    }

    @Provides
    @Singleton
    ZkClusterLockHandler provideZkLockHandler(Provider<CuratorFramework> curatorFramework) {
        return new ZkClusterLockHandler(curatorFramework);
    }
}
