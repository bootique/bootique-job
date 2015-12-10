package com.nhl.bootique.job.lock;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.nhl.bootique.job.JobMetadata;
import com.nhl.bootique.job.runnable.JobOutcome;
import com.nhl.bootique.job.runnable.JobResult;
import com.nhl.bootique.job.runnable.RunnableJob;

public class ZkClusterLockHandler implements LockHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZkClusterLockHandler.class);

	// TODO: using a shared library package name for job locking can create
	// unneeded contention between different apps
	private final static String ZK_PATH_PREFIX = "/"
			+ ZkClusterLockHandler.class.getPackage().getName().replace('.', '/') + "/";

	private final Provider<CuratorFramework> zkClient;

	@Inject
	public ZkClusterLockHandler(Provider<CuratorFramework> zkClient) {
		this.zkClient = zkClient;
	}

	@Override
	public RunnableJob lockingJob(RunnableJob executable, JobMetadata metadata) {

		return () -> {
			String lockName = getLockName(metadata);
			InterProcessMutex lock = getLock(lockName);

			LOGGER.info("Attempting to lock '{}'", lockName);
			if (!acquire(lock)) {
				LOGGER.info("** Another job instance owns the lock. Skipping execution of '{}'", lockName);
				return new JobResult(metadata, JobOutcome.SKIPPED, null,
						"Another job instance owns the lock. Skipping execution");
			}

			try {
				return executable.run();
			} finally {
				release(lock);
			}
		};
	}

	protected boolean acquire(InterProcessMutex lock) {
		try {
			return lock.acquire(2, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Exception acquiring Zookeeper lock", e);
		}
	}

	protected void release(InterProcessMutex lock) {
		try {
			lock.release();
		} catch (Exception e) {
			throw new RuntimeException("Exception releasing Zookeeper lock", e);
		}
	}

	private InterProcessMutex getLock(String lockName) {
		// do not cache the locks, as this would break locking within
		// the same VM
		return new InterProcessMutex(zkClient.get(), lockName);
	}

	private String getLockName(JobMetadata metadata) {
		return ZK_PATH_PREFIX + metadata.getName();
	}
}
