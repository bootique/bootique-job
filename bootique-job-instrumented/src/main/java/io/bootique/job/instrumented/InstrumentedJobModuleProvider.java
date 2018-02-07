package io.bootique.job.instrumented;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.job.runtime.JobModuleProvider;
import io.bootique.metrics.MetricsModuleProvider;

import java.util.Collection;

import static java.util.Arrays.asList;

/**
 * @since 0.14
 */
public class InstrumentedJobModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new InstrumentedJobModule();
    }

    @Override
    public Collection<BQModuleProvider> dependencies() {
        return asList(
                new JobModuleProvider(),
                new MetricsModuleProvider()
        );
    }
}
