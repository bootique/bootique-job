package io.bootique.job.zookeeper;

import io.bootique.BQModuleProvider;
import io.bootique.di.BQModule;
import io.bootique.job.runtime.JobModule;

import java.util.Collection;
import java.util.Collections;

public class ZkJobModuleProvider implements BQModuleProvider {

    @Override
    public BQModule module() {
        return new ZkJobModule();
    }

    @Override
    public Collection<Class<? extends BQModule>> overrides() {
        return Collections.singleton(JobModule.class);
    }
}
