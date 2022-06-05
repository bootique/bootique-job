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

package io.bootique.job;

import io.bootique.command.CommandOutcome;
import io.bootique.job.fixture.ExecutableAtMostOnceJob;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

@BQTest
public abstract class BaseJobExecIT {

    @BQTestTool
    protected final BQTestFactory testFactory = new BQTestFactory();

    protected CommandOutcome executeJobs(Collection<? extends Job> jobs, String... args) {
        return testFactory.app(args)
                .autoLoadModules()
                .module(b -> JobModule.extend(b).config(e -> jobs.forEach(e::addJob)))
                .run();
    }

    protected void assertSuccess(CommandOutcome outcome) {
        assertTrue(outcome.isSuccess(), "Execution command was expected to succeed, but hasn't");
        assertEquals(0, outcome.getExitCode());
    }

    protected void assertFailed(CommandOutcome outcome) {
        assertFalse(outcome.isSuccess(), "Execution command was expected to fail, but hasn't");
        assertNotEquals(0, outcome.getExitCode());
    }

    protected void assertExecutedInOrder(List<ExecutableAtMostOnceJob> jobs) {
        if (jobs.isEmpty()) {
            return;
        }

        assertExecuted(jobs);

        List<ExecutableAtMostOnceJob> sorted = new ArrayList<>(jobs);
        sorted.sort((j1, j2) -> {
            long diff = j1.getStartedAt() - j2.getStartedAt();
            assertNotEquals(0, diff, () -> "Jobs started at the same time: " + collectNames(j1, j2));
            return (int) diff;
        });

        for (int i = 1; i < sorted.size(); i++) {
            ExecutableAtMostOnceJob p = sorted.get(i - 1);
            ExecutableAtMostOnceJob n = sorted.get(i);
            assertTrue(p.getFinishedAt() < n.getStartedAt(), "Execution of jobs overlapped: " + collectNames(p, n));
        }

        try {
            assertArrayEquals(jobs.toArray(), sorted.toArray());
        } catch (Throwable e) {
            throw new RuntimeException("Expected: " + collectNames(jobs) + ", actual: " + collectNames(sorted));
        }
    }

    protected void assertExecuted(List<ExecutableAtMostOnceJob> jobs) {
        jobs.forEach(job -> assertTrue(job.isExecuted(), "Job was not executed: " + job.getMetadata().getName()));
    }

    protected void assertNotExecuted(List<ExecutableAtMostOnceJob> jobs) {
        jobs.forEach(job -> assertFalse(job.isExecuted(), "Job was executed: " + job.getMetadata().getName()));
    }

    protected void assertExecutedWithParams(ExecutableAtMostOnceJob job, Map<String, Object> expectedParams) {
        assertExecuted(Collections.singletonList(job));
        assertEquals(expectedParams, job.getParams());
    }

    @SafeVarargs
    private static <T extends Job> String collectNames(T... jobs) {
        return collectNames(asList(jobs));
    }

    private static String collectNames(List<? extends Job> jobs) {
        return jobs.stream().
                map(j -> j.getMetadata().getName())
                .collect(Collectors.joining(","));
    }
}
