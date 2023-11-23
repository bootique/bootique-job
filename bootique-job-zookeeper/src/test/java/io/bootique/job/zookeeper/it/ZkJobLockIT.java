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
package io.bootique.job.zookeeper.it;

import io.bootique.job.Scheduler;
import io.bootique.job.zookeeper.it.job.LockJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZkJobLockIT extends AbstractZkIT {

    public static final String CALLS_COUNT = "count";
    private static final int WAIT_TIME = 15_000;

    private Map<String, Object> callsCount;

    @BeforeEach
    public void before() {
        callsCount = new ConcurrentHashMap<>();
        callsCount.put(CALLS_COUNT, 0);
    }

    @Test
    public void zkClusterLock() throws InterruptedException {
        Scheduler scheduler_1 = getSchedulerFromRuntime();
        Scheduler scheduler_2 = getSchedulerFromRuntime();
        scheduler_1.runBuilder().job(new LockJob()).params(callsCount).runNonBlocking();
        scheduler_2.runBuilder().job(new LockJob()).params(callsCount).runNonBlocking();
        Thread.sleep(WAIT_TIME);
        assertEquals(1, callsCount.get(CALLS_COUNT));
    }

    @Test
    public void zkLocalLock() throws InterruptedException {
        Scheduler scheduler = getSchedulerFromRuntime();
        scheduler.runBuilder().job(new LockJob()).params(callsCount).runNonBlocking();
        scheduler.runBuilder().job(new LockJob()).params(callsCount).runNonBlocking();
        Thread.sleep(WAIT_TIME);
        assertEquals(1, callsCount.get(CALLS_COUNT));
    }
}
