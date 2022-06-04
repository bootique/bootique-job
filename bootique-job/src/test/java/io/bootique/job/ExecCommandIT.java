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
import io.bootique.job.fixture.Job1;
import io.bootique.job.fixture.Job2;
import io.bootique.job.fixture.Job3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExecCommandIT extends BaseJobExecIT {

    @Test
    public void testSingleJob() {
        Job1 job1 = new Job1(0);
        executeJobs(Collections.singleton(job1), "--exec", "--job=job1");
        assertTrue(job1.isExecuted());
    }

    @Test
    public void testSingleJob_Failure() {
        Job1 job1 = new Job1(0, true);
        CommandOutcome outcome = executeJobs(Collections.singleton(job1), "--exec", "--job=job1");
        assertTrue(job1.isExecuted());
        assertFailed(outcome);
    }

    @Test
    public void testMultipleJobs_Parallel() {
        testMultipleJobs(false, false);
    }

    @Test
    public void testMultipleJobs_Parallel_Failure() {
        testMultipleJobs(false, true);
    }

    @Test
    public void testMultipleJobs_Serial() {
        testMultipleJobs(true, false);
    }

    @Test
    public void testMultipleJobs_Serial_Failure() {
        testMultipleJobs(true, true);
    }

    /**
     * 1. jobs are submitted to the executor in the same order as they appear in program arguments
     * 2. jobs are more likely to be executed in the same order in which they are submitted to the executor
     * => to increase the prob. of other orders of execution (incl. overlapping executions)
     * we make the first submitted job the most time-consuming
     **/
    private void testMultipleJobs(boolean serial, boolean shouldFail) {
        List<String> args = new ArrayList<>(asList("--exec", "--job=job1", "--job=job2", "--job=job3"));

        if (serial) {
            args.add("--serial");
        }

        int jobCount = 3;

        for (int i = 0; i < 100; i++) {
            boolean firstShouldFail = shouldFail && randomizedBoolean(jobCount);
            boolean secondShouldFail = shouldFail && randomizedBoolean(jobCount);
            boolean thirdShouldFail = shouldFail && randomizedBoolean(jobCount);

            if (shouldFail && !firstShouldFail && !secondShouldFail && !thirdShouldFail) {
                switch (new Random().nextInt(3)) {
                    case 0:
                        firstShouldFail = true;
                        break;
                    case 1:
                        secondShouldFail = true;
                        break;
                    case 2:
                        thirdShouldFail = true;
                        break;
                }
            }

            List<ExecutableAtMostOnceJob> jobs = List.of(
                    new Job1(100000, firstShouldFail),
                    new Job2(10000, secondShouldFail),
                    new Job3(1000, thirdShouldFail));

            CommandOutcome outcome = executeJobs(jobs, args.toArray(new String[0]));

            if (serial) {
                List<ExecutableAtMostOnceJob> expectedToExecute = new ArrayList<>();
                List<ExecutableAtMostOnceJob> notExpectedToExecute = new ArrayList<>();

                boolean previousHasFailed = false;
                for (ExecutableAtMostOnceJob job : jobs) {
                    if (!previousHasFailed && job.shouldFail()) {
                        expectedToExecute.add(job);
                    } else if (previousHasFailed) {
                        notExpectedToExecute.add(job);
                    } else {
                        expectedToExecute.add(job);
                    }
                    previousHasFailed = previousHasFailed || job.shouldFail();
                }

                assertExecutedInOrder(expectedToExecute);
                assertNotExecuted(notExpectedToExecute);
            } else {
                assertExecuted(jobs);
            }

            if (shouldFail) {
                assertFailed(outcome);
            } else {
                assertSuccess(outcome);
            }
        }
    }

    private boolean randomizedBoolean(int inverseProb) {
        return new Random().nextInt(inverseProb) == 0;
    }

    @Test
    public void testMultipleGroups_Parallel() {
        String[] args = new String[]{
                "--config=classpath:io/bootique/job/config_exec.yml",
                "--exec",
                "--job=group2",
                "--job=group1"};

        for (int i = 0; i < 100; i++) {
            List<ExecutableAtMostOnceJob> jobs = List.of(new Job3(10000), new Job2(1000), new Job1());
            CommandOutcome outcome = executeJobs(jobs, args);
            assertExecuted(jobs);
            assertSuccess(outcome);
        }
    }

    @Test
    public void testMultipleGroups_Serial() {
        String[] args = new String[]{
                "--config=classpath:io/bootique/job/config_exec.yml",
                "--exec",
                "--job=group2",
                "--job=group1",
                "--serial"};

        for (int i = 0; i < 100; i++) {
            List<ExecutableAtMostOnceJob> jobs = List.of(new Job3(10000), new Job2(1000), new Job1());
            CommandOutcome outcome = executeJobs(jobs, args);
            assertExecutedInOrder(jobs);
            assertSuccess(outcome);
        }
    }
}
