package io.bootique.job.instrumented;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;

/**
 * @since 0.14
 */
public class InstrumentedJobModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new InstrumentedJobModule();
    }
}
