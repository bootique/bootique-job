package io.bootique.job.lock.zookeeper;

import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.BaseRunnableJob;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.RunnableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.runnable.JobResult;

import java.util.Map;

public class ZkClusterLockHandler implements LockHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZkClusterLockHandler.class);

	// TODO: using a shared library package name for job locking can create
	// unneeded contention between different apps
	private final static String ZK_PATH_PREFIX = "/"
			+ ZkClusterLockHandler.class.getPackage().getName().replace('.', '/') + "/";

	private final Injector injector;

	@Inject
	public ZkClusterLockHandler(Injector injector) {
		this.injector = injector;
	}

	@Override
	public RunnableJob lockingJob(RunnableJob executable, JobMetadata metadata) {
		return new BaseRunnableJob() {
			@Override
			protected JobResult doRun() {
				String lockName = getLockName(metadata);

				LOGGER.info("Attempting to lock '{}'", lockName);

				ZkMutex lock = ZkMutex.acquire(injector, lockName);
				if (lock == null) {
					LOGGER.info("** Another job instance owns the lock. Skipping execution of '{}'", lockName);
					return new JobResult(metadata, JobOutcome.SKIPPED, null,
							"Another job instance owns the lock. Skipping execution");
				}

				try {
					return executable.run();
				} finally {
					lock.release();
				}
			}

			@Override
			public Map<String, Object> getParameters() {
				return executable.getParameters();
			}
		};
	}

	private String getLockName(JobMetadata metadata) {
		return ZK_PATH_PREFIX + metadata.getName();
	}
}
