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

package io.bootique.job.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJob;

/**
 * Cluster unaware {@link LockHandler}.
 */
public class LocalLockHandler implements LockHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalLockHandler.class);

	private final ConcurrentMap<String, Lock> locks;

	public LocalLockHandler() {
		this.locks = new ConcurrentHashMap<>();
	}

	@Override
	public RunnableJob lockingJob(RunnableJob executable, JobMetadata metadata) {

		return () -> {
			String lockName = toLockName(metadata);
			Lock lock = getLock(lockName);

			LOGGER.info("Attempting to lock '{}'", lockName);

			if (!lock.tryLock()) {
				LOGGER.info("== Another job instance owns the lock. Skipping execution of '{}'", lockName);
				return new JobResult(metadata, JobOutcome.SKIPPED, null,
						"Another job instance owns the lock. Skipping execution");
			}

			try {
				return executable.run();
			} finally {
				lock.unlock();
			}
		};
	}

	private Lock getLock(String lockName) {
		return locks.computeIfAbsent(lockName, k -> new ReentrantLock());
	}

	private String toLockName(JobMetadata metadata) {
		return metadata.getLockName();
	}
}
