package io.bootique.job.zookeeper;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class ConsulJobModuleProviderTest {
	
	@Test
	public void testAutoLoadable() {
		BQModuleProviderChecker.testAutoLoadable(ZkJobModuleProvider.class);
	}
}
