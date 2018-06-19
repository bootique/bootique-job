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
import io.bootique.job.runtime.JobModule;
import io.bootique.job.runtime.JobModuleExtender;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public abstract class BaseJobExecIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    protected CommandOutcome executeJobs(Collection<? extends Job> jobs, String... args) {
        return testFactory.app(args)
                .module(new JobModule())
                .module(binder -> {
                    JobModuleExtender extender = JobModule.extend(binder);
                    jobs.forEach(extender::addJob);
                }).createRuntime()
                .run();
    }

    protected void assertSuccess(CommandOutcome outcome) {
        assertTrue("Execution command was expected to succeed, but hasn't", outcome.isSuccess());
        assertEquals(0, outcome.getExitCode());
    }

    protected void assertFailed(CommandOutcome outcome) {
        assertFalse("Execution command was expected to fail, but hasn't", outcome.isSuccess());
        assertNotEquals(0, outcome.getExitCode());
    }

    protected void assertExecutedInOrder(List<ExecutableAtMostOnceJob> jobs) {
        if (jobs.isEmpty()) {
            return;
        }

        assertExecuted(jobs);

        List<ExecutableAtMostOnceJob> jobList = new ArrayList<>(jobs);
        jobList.sort((j1, j2) -> {
            long diff = j1.getStartedAt() - j2.getStartedAt();
            assertNotEquals("Jobs started at the same time: " + collectNames(j1, j2), 0, diff);
            return (int) diff;
        });

        Iterator<ExecutableAtMostOnceJob> iter = jobList.iterator();
        ExecutableAtMostOnceJob previous = iter.next(), next;
        while (iter.hasNext()) {
            next = iter.next();
            assertTrue("Execution of jobs overlapped: " + collectNames(previous, next),
                    previous.getFinishedAt() < next.getStartedAt());
        }

        try {
            assertArrayEquals(jobs.toArray(), jobList.toArray());
        } catch (Throwable e) {
            throw new RuntimeException("Expected: " + collectNames(jobs) + ", actual: " + collectNames(jobList));
        }
    }

    protected void assertExecuted(List<ExecutableAtMostOnceJob> jobs) {
        jobs.forEach(job -> assertTrue("Job was not executed: " + job.getMetadata().getName(), job.isExecuted()));
    }

    protected void assertNotExecuted(List<ExecutableAtMostOnceJob> jobs) {
        jobs.forEach(job -> assertFalse("Job was executed: " + job.getMetadata().getName(), job.isExecuted()));
    }

    protected void assertExecutedWithParams(ExecutableAtMostOnceJob job, Map<String, Object> expectedParams) {
        assertExecuted(Collections.singletonList(job));
        assertEquals(expectedParams, job.getParams());
    }

    @SafeVarargs
    private static <T extends Job> String collectNames(T... jobs) {
        return collectNames(Arrays.asList(jobs));
    }

    private static String collectNames(List<? extends Job> jobs) {
        return jobs.stream().collect(
                StringBuilder::new,
                (acc, job) -> acc.append(acc.length() > 0 ? "," + job.getMetadata().getName() : job.getMetadata().getName()),
                (acc1, acc2) -> acc1.append(acc2.toString())).toString();
    }
}
