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

import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.zookeeper.it.job.LockJob;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZkJobLockIT extends AbstractZkIT {

    private static final String CONFIG_PATH = "--config=classpath:io/bootique/job/zookeeper/it/job-lock.yml";
    public static final String CALLS_COUNT = "count";
    private static final int WAIT_TIME = 8_000;

    private Map<String, Object> callsCount;

    @Before
    public void before() {
        callsCount = new ConcurrentHashMap<>();
        callsCount.put(CALLS_COUNT, 0);
    }

    @Test
    public void testZkClusterLock() throws InterruptedException {
        Scheduler scheduler_1 = getSchedulerFromRuntime(CONFIG_PATH);
        Scheduler scheduler_2 = getSchedulerFromRuntime(CONFIG_PATH);
        scheduler_1.runOnce(new LockJob(), callsCount);
        scheduler_2.runOnce(new LockJob(), callsCount);
        Thread.sleep(WAIT_TIME);
        Assert.assertEquals(1, callsCount.get(CALLS_COUNT));
    }

    @Test
    public void testZkLocalLock() throws InterruptedException {
        Scheduler scheduler = getSchedulerFromRuntime(CONFIG_PATH);
        scheduler.runOnce(new LockJob(), callsCount);
        scheduler.runOnce(new LockJob(), callsCount);
        Thread.sleep(WAIT_TIME);
        Assert.assertEquals(1, callsCount.get(CALLS_COUNT));
    }
}
