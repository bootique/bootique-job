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

import java.util.concurrent.TimeUnit;

import io.bootique.di.Injector;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

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
