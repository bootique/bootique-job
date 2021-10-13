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

package io.bootique.job.scheduler;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.job.fixture.ParameterizedJob3;
import io.bootique.job.fixture.SerialJob1;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runtime.JobModule;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@BQTest
public class Scheduler_JobGroupIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler_JobGroupIT.class);

    private ExecutorService executor;

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app("-c", "classpath:io/bootique/job/config_jobgroup.yml")
            .module(JobModule.class)
            .module(b -> JobModule.extend(b).addJob(SerialJob1.class))
            .module(b -> JobModule.extend(b).addJob(ParameterizedJob3.class))
            .createRuntime();

    @BeforeEach
    public void setUp() {
        executor = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testRunOnce_JobGroup() throws InterruptedException {
        Scheduler scheduler = app.getInstance(Scheduler.class);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("param2", 2);

        Queue<JobResult> resultQueue = new LinkedBlockingQueue<>(1);
        CountDownLatch latch = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                resultQueue.add(scheduler.runOnce("group1", parameters).get());
            } finally {
                LOGGER.info(resultQueue.element().toString());
                latch.countDown();
            }
        });

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        if (!finished) {
            fail("Timeout while waiting for job execution. Still left: " + latch.getCount());
        }

        assertEquals(1, resultQueue.size());
        assertEquals(JobOutcome.SUCCESS, resultQueue.peek().getOutcome());
    }

    @Test
    public void testRunOnce_EmptyGroup() throws InterruptedException {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        Queue<JobResult> resultQueue = new LinkedBlockingQueue<>(1);
        CountDownLatch latch = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                resultQueue.add(scheduler.runOnce("group2", Collections.emptyMap()).get());
            } finally {
                LOGGER.info(resultQueue.element().toString());
                latch.countDown();
            }
        });

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        if (!finished) {
            fail("Timeout while waiting for job execution. Still left: " + latch.getCount());
        }

        assertEquals(1, resultQueue.size());
        assertEquals(JobOutcome.SUCCESS, resultQueue.peek().getOutcome());
    }
}
