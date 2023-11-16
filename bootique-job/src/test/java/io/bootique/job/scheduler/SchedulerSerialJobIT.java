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
import io.bootique.job.JobModule;
import io.bootique.job.JobOutcome;
import io.bootique.job.JobResult;
import io.bootique.job.Scheduler;
import io.bootique.job.fixture.SerialJob1;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class SchedulerSerialJobIT {

    private ExecutorService executor;

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .module(JobModule.class)
            .module(b -> JobModule.extend(b).addJob(SerialJob1.class))
            .createRuntime();

    @BeforeEach
    public void startExecutor() {
        this.executor = Executors.newFixedThreadPool(5);
    }

    @AfterEach
    public void stopExecutor() {
        executor.shutdownNow();
    }

    @Test
    public void testSerialJob() throws InterruptedException {
        Scheduler scheduler = app.getInstance(Scheduler.class);
        final int count = 5;

        ConcurrentMap<JobOutcome, AtomicInteger> outcomes = new ConcurrentHashMap<>();
        for (JobOutcome o : JobOutcome.values()) {
            outcomes.put(o, new AtomicInteger(0));
        }

        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            executor.submit(() -> {
                try {
                    JobResult r = scheduler.runBuilder().jobName("serialjob1").runNonBlocking().get();
                    outcomes.get(r.getOutcome()).incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean allRun = latch.await(10, TimeUnit.SECONDS);
        if (!allRun) {
            fail("Timeout while waiting for job execution. Still left: " + latch.getCount());
        }

        // The test is probabilistic... Some jobs may not have been scheduled yet, when the first
        // job already finished. So, there may be some failures (because 'serialjob1' allows to be run only once),
        // but all we need to check is that there were at least some skips.
        // TODO: Even that does not guarantee the test would always pass. E.g. on a single CPU machine it would
        //  probably fail
        assertEquals(1, outcomes.get(JobOutcome.SUCCESS).get(), "No jobs finished successfully, expected exactly one");
        assertTrue(outcomes.get(JobOutcome.SKIPPED).get() > 0, "At least some jobs should have been skipped");
    }
}
