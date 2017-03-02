package io.bootique.job.instrumented;

import com.google.inject.Binder;
import io.bootique.ConfigModule;
import io.bootique.job.runtime.JobModule;

/**
 * @since 0.14
 */
public class InstrumentedJobModule extends ConfigModule {

    public InstrumentedJobModule() {

    }

    public InstrumentedJobModule(String configPrefix) {
        super(configPrefix);
    }

    @Override
    public void configure(Binder binder) {
        JobModule.extend(binder).addListener(InstrumentedJobListener.class);
    }
}
