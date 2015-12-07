package com.nhl.launcher.job.locking;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.launcher.job.JobMetadata;
import com.nhl.launcher.job.runnable.JobOutcome;
import com.nhl.launcher.job.runnable.JobResult;
import com.nhl.launcher.job.runnable.RunnableJob;

/**
 * Cluster unaware {@link SerialJobRunner}.
 */
public class LocalSerialJobRunner implements SerialJobRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalSerialJobRunner.class);

	private final ConcurrentMap<String, Lock> locks;

	public LocalSerialJobRunner() {
		this.locks = new ConcurrentHashMap<>();
	}

	@Override
	public JobResult runSerially(RunnableJob runnable, JobMetadata metadata) {
		String lockName = toLockName(metadata);
		Lock lock = getLock(lockName);

		LOGGER.info("Attempting to lock '{}'", lockName);

		if (!lock.tryLock()) {
			LOGGER.info("== Another job instance owns the lock. Skipping execution of '{}'", lockName);
			return new JobResult(metadata, JobOutcome.SKIPPED, null,
					"Another job instance owns the lock. Skipping execution");
		}

		try {
			return runnable.run();
		} finally {
			lock.unlock();
		}
	}

	private Lock getLock(String lockName) {
		return locks.computeIfAbsent(lockName, k -> new ReentrantLock());
	}

	private String toLockName(JobMetadata metadata) {
		return metadata.getName();
	}
}
