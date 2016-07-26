package io.bootique.job.runtime;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class JobModuleProviderIT {
	
	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(JobModuleProvider.class);
	}
}
