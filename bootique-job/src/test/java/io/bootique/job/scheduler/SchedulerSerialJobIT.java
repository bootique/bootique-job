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
import io.bootique.job.*;
import io.bootique.job.fixture.SerialJob2;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@BQTest
public class SchedulerSerialJobIT {

    private ExecutorService executor;

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .modules(new JobsModule(), new SchedulerModule())
            .module(b -> JobsModule.extend(b).addJob(SerialJob2.class))
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
    public void serialJob() throws InterruptedException {
        Scheduler scheduler = app.getInstance(Scheduler.class);
        final int count = 5;

        ConcurrentMap<Integer, JobOutcome> results = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            Integer id = i;
            executor.submit(() -> {
                try {
                    JobOutcome r = scheduler.runBuilder().jobName("serialjob2").runNonBlocking().get();
                    results.put(id, r);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean allRun = latch.await(10, TimeUnit.SECONDS);
        if (!allRun) {
            fail("Timeout while waiting for job execution. Still left: " + latch.getCount());
        }

        // overlapping jobs should be canceled, and only non-overlapping can succeed.

        List<Long[]> successRanges = results.values().stream()

                // can either be non-overlapping successes or skipped
                .peek(r -> assertTrue(r.getStatus() == JobStatus.SUCCESS || r.getStatus() == JobStatus.SKIPPED))

                // check for success overlaps
                .filter(r -> r.getStatus() == JobStatus.SUCCESS)
                .map(r -> r.getMessage().split(":"))
                .map(ss -> new Long[]{Long.parseLong(ss[0]), Long.parseLong(ss[1])})
                .sorted(Comparator.comparing(ll -> ll[0]))
                .collect(Collectors.toList());

        assertTrue(!successRanges.isEmpty());
        for (int i = 1; i < successRanges.size(); i++) {
            assertTrue(successRanges.get(i - 1)[1] < successRanges.get(i)[0]);
        }
    }
}
