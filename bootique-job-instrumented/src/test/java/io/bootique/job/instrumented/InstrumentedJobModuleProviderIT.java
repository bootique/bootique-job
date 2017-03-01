package io.bootique.job.instrumented;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class InstrumentedJobModuleProviderIT {

	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(InstrumentedJobModuleProvider.class);
	}
}
