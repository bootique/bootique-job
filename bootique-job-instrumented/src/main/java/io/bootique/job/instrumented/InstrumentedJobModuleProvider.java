package io.bootique.job.instrumented;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.job.runtime.JobModule;

import java.util.Collection;
import java.util.Collections;

/**
 * @since 0.14
 */
public class InstrumentedJobModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new InstrumentedJobModule();
    }

    @Override
    public Collection<Class<? extends Module>> overrides() {
        return Collections.singleton(JobModule.class);
    }
}
