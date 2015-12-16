package com.nhl.bootique.job.lock.zookeeper;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import com.google.inject.Injector;

/**
 * Wraps Curator InterProcessMutex, so that {@link ZkClusterLockHandler} does
 * not have direct class loading dependency on Curator.
 */
class ZkMutex {

	private InterProcessMutex lock;

	static ZkMutex acquire(Injector injector, String lockName) {
		CuratorFramework curator = injector.getInstance(CuratorFramework.class);
		InterProcessMutex lock = new InterProcessMutex(curator, lockName);

		return acquire(lock) ? new ZkMutex(lock) : null;
	}

	private static boolean acquire(InterProcessMutex lock) {
		try {
			return lock.acquire(2, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Exception acquiring Zookeeper lock", e);
		}
	}

	private ZkMutex(InterProcessMutex lock) {
		this.lock = lock;
	}

	void release() {
		try {
			lock.release();
		} catch (Exception e) {
			throw new RuntimeException("Exception releasing Zookeeper lock", e);
		}
	}

}
