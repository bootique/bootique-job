package com.nhl.bootique.job.runtime;

import org.junit.Test;

import com.nhl.bootique.test.junit.BQModuleProviderChecker;

public class JobModuleProviderIT {
	
	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(JobModuleProvider.class);
	}
}
