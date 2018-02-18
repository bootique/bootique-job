package io.bootique.job.instrumented;

import io.bootique.BQRuntime;
import io.bootique.job.runtime.JobModule;
import io.bootique.metrics.MetricsModule;
import io.bootique.test.junit.BQModuleProviderChecker;
import io.bootique.test.junit.BQRuntimeChecker;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

public class InstrumentedJobModuleProviderIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testAutoLoadable() {
        BQModuleProviderChecker.testAutoLoadable(InstrumentedJobModuleProvider.class);
    }

    @Test
    public void testModuleDeclaresDependencies() {
        final BQRuntime bqRuntime = testFactory.app().module(new InstrumentedJobModuleProvider()).createRuntime();
        BQRuntimeChecker.testModulesLoaded(bqRuntime, JobModule.class, MetricsModule.class);
    }
}
