package io.bootique.job.zookeeper;

import io.bootique.BQModuleProvider;
import io.bootique.bootstrap.BuiltModule;

public class ZkJobModuleProvider implements BQModuleProvider {

    @Override
    public BuiltModule buildModule() {
        return BuiltModule.of(new ZkJobModule())
                .provider(this)
                .description("Integrates Zookeeper-based Bootique job locks")
                .build();
    }
}
