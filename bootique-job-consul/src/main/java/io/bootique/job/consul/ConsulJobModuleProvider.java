package io.bootique.job.consul;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.job.runtime.JobModule;

import java.util.Collection;
import java.util.Collections;

/**
 * @since 0.26
 */
public class ConsulJobModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new ConsulJobModule();
    }

    @Override
    public Collection<Class<? extends Module>> overrides() {
        return Collections.singleton(JobModule.class);
    }
}
