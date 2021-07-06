/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.job.zookeeper.lock;

import javax.inject.Inject;

import io.bootique.di.Injector;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.RunnableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bootique.job.lock.LockHandler;
import io.bootique.job.runnable.JobResult;

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

		return () -> {
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
		};
	}

	private String getLockName(JobMetadata metadata) {
		return ZK_PATH_PREFIX + metadata.getLockName();
	}
}
