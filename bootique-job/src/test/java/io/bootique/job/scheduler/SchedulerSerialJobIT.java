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

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class SchedulerSerialJobIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerSerialJobIT.class);

    private ExecutorService executor;

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .module(JobModule.class)
            .module(b -> JobModule.extend(b).addJob(SerialJob1.class))
            .createRuntime();

    @BeforeEach
    public void setUp() {
        this.executor = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testSerialJob() throws InterruptedException {
        String jobName = "serialjob1";
        Scheduler scheduler = app.getInstance(Scheduler.class);
        int count = 10;

        Queue<JobResult> resultQueue = new LinkedBlockingQueue<>(count + 1);
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            executor.submit(() -> {
                try {
                    resultQueue.add(scheduler.runOnce(jobName).get());
                } catch (Exception e) {
                    LOGGER.error("Failed to run job", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean allRun = latch.await(10, TimeUnit.SECONDS);

        // check if any of the job instances hanged up
        if (!allRun) {
            fail("Timeout while waiting for job execution. Still left: " + latch.getCount());
        }

        // verify that all jobs have finished execution without throwing an exception
        assertEquals(count, resultQueue.size());

        // one of the jobs is expected to finish successfully
        // (which one is unknown though, because scheduler also uses an executor internally)
        boolean foundOneSuccessful = false;
        Iterator<JobResult> iter = resultQueue.iterator();
        while (iter.hasNext()) {
            JobResult result = iter.next();
            if (result.getOutcome() == JobOutcome.SUCCESS) {
                iter.remove();
                foundOneSuccessful = true;
                break;
            }
        }

        assertTrue(foundOneSuccessful, "No jobs finished successfully, expected exactly one");

        for (int i = 1; i < count; i++) {
            // we expect all other simultaneous jobs to be skipped by scheduler;
            // otherwise we expect failure, because io.bootique.job.fixture.ExecutableJob
            // throws an exception if run more than once
            JobOutcome actualOutcome = resultQueue.poll().getOutcome();
            assertEquals(JobOutcome.SKIPPED, actualOutcome, "Execution #" + (i + 1) + " was not skipped; actual outcome: " + actualOutcome);
        }
    }
}