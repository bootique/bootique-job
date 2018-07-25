package io.bootique.job.zookeeper;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.job.runtime.JobModule;

import java.util.Collection;
import java.util.Collections;

public class ZkJobModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new ZkJobModule();
    }

    @Override
    public Collection<Class<? extends Module>> overrides() {
        return Collections.singleton(JobModule.class);
    }
}
