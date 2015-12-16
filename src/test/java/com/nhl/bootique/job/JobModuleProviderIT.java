package com.nhl.bootique.job;

import static org.junit.Assert.assertTrue;

import java.util.ServiceLoader;

import org.junit.Test;

import com.nhl.bootique.BQModuleProvider;

public class JobModuleProviderIT {
	@Test
	public void testPresentInJar() {

		boolean[] found = { false };

		ServiceLoader.load(BQModuleProvider.class).forEach(p -> {
			if (p instanceof JobModuleProvider) {
				found[0] = true;
			}
		});

		assertTrue(found[0]);
	}
}
