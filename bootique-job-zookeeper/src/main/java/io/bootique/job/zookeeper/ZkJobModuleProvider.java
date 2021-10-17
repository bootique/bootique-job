package io.bootique.job.zookeeper;

import io.bootique.BQModuleProvider;
import io.bootique.di.BQModule;

public class ZkJobModuleProvider implements BQModuleProvider {

    @Override
    public BQModule module() {
        return new ZkJobModule();
    }
}
