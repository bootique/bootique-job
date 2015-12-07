package com.nhl.launcher.job.locking;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.nhl.launcher.job.JobMetadata;
import com.nhl.launcher.job.runnable.JobOutcome;
import com.nhl.launcher.job.runnable.JobResult;
import com.nhl.launcher.job.runnable.RunnableJob;

public class ZkClusterSerialJobRunner implements SerialJobRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZkClusterSerialJobRunner.class);

	// TODO: using a shared library package name for job locking can create
	// unneeded contention between different apps
	private final static String ZK_PATH_PREFIX = "/"
			+ ZkClusterSerialJobRunner.class.getPackage().getName().replace('.', '/') + "/";

	private final CuratorFramework zkClient;

	@Autowired
	public ZkClusterSerialJobRunner(CuratorFramework zkClient) {
		this.zkClient = zkClient;
	}

	@Override
	public JobResult runSerially(RunnableJob runnable, JobMetadata metadata) {
		String lockName = getLockName(metadata);
		InterProcessMutex lock = getLock(lockName);

		LOGGER.info("Attempting to lock '{}'", lockName);
		if (!acquire(lock)) {
			LOGGER.info("** Another job instance owns the lock. Skipping execution of '{}'", lockName);
			return new JobResult(metadata, JobOutcome.SKIPPED, null,
					"Another job instance owns the lock. Skipping execution");
		}

		try {
			return runnable.run();
		} finally {
			release(lock);
		}
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
		return new InterProcessMutex(zkClient, lockName);
	}

	private String getLockName(JobMetadata metadata) {
		return ZK_PATH_PREFIX + metadata.getName();
	}
}
