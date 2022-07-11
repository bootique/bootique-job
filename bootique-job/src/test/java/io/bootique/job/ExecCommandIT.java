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

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.BootiqueException;
import io.bootique.command.CommandOutcome;
import io.bootique.di.DIRuntimeException;
import io.bootique.job.fixture.*;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

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

    @RepeatedTest(10)
    public void testMultipleGroups_Parallel() {
        List<ExecutableAtMostOnceJob> jobs = List.of(new Job3(10000), new Job2(1000), new Job1());
        CommandOutcome outcome = executeJobs(jobs,
                "--config=classpath:io/bootique/job/config_exec.yml",
                "--exec",
                "--job=group2",
                "--job=group1");
        assertExecuted(jobs);
        assertSuccess(outcome);
    }

    @RepeatedTest(10)
    public void testMultipleGroups_Serial() {
        List<ExecutableAtMostOnceJob> jobs = List.of(new Job3(10000), new Job2(1000), new Job1());
        CommandOutcome outcome = executeJobs(jobs,
                "--config=classpath:io/bootique/job/config_exec.yml",
                "--exec",
                "--job=group2",
                "--job=group1",
                "--serial");
        assertExecutedInOrder(jobs);
        assertSuccess(outcome);
    }

    @Test
    public void testSingleJob_DefaultParams() {
        Job1 j1 = new Job1();
        Job2 j2 = new Job2();
        Job3 j3 = new Job3(10000);

        executeJobs(List.of(j1, j2, j3), "--config=classpath:io/bootique/job/config.yml", "--exec", "--job=job1");
        assertExecuted(List.of(j1));
    }

    @Test
    public void testGroup1_SingleJob_DefaultParams() {
        Job1 j1 = new Job1();
        Job2 j2 = new Job2();
        Job3 j3 = new Job3(10000);

        executeJobs(List.of(j1, j2, j3), "--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group1");
        assertExecuted(List.of(j1));
    }

    @Test
    public void testGroup6_SingleJob_OverriddenParams() {
        Job1 j1 = new Job1();
        Job2 j2 = new Job2();
        Job3 j3 = new Job3(10000);

        executeJobs(List.of(j1, j2, j3), "--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group6");
        assertExecutedWithParams(j1, Map.of("a", "overridden", "b", "default"));
    }

    @Test
    public void testGroup2_MultipleJobs_Parallel_DefaultParams() {
        Job1 j1 = new Job1();
        Job2 j2 = new Job2();
        Job3 j3 = new Job3(10000);

        executeJobs(List.of(j1, j2, j3), "--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group2");
        assertExecuted(List.of(j1, j2));
    }

    @Test
    public void testGroup3_MultipleJobs_Parallel_OverriddenParams() {
        Job1 j1 = new Job1();
        Job2 j2 = new Job2();
        Job3 j3 = new Job3(10000);

        executeJobs(List.of(j1, j2, j3), "--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group3");
        assertExecutedWithParams(j1, Map.of("a", "overridden", "b", "default"));
        assertExecutedWithParams(j2, Map.of("e", "default", "y", "added"));
    }

    @Test
    public void testGroup4_MultipleJobs_Dependent_OverriddenParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(1000);

        executeJobs(List.of(job1, job2, job3), "--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group4");
        assertExecutedInOrder(List.of(job2, job1));
        assertExecutedWithParams(job2, Map.of("e", "overridden"));
    }

    @Test
    public void testGroup5_MultipleJobs_Dependent_OverriddenParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);

        List<ExecutableAtMostOnceJob> jobs = List.of(job3, job2, job1);
        executeJobs(jobs, "--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group5");
        assertExecutedInOrder(jobs);
        assertExecutedWithParams(job1, Map.of("a", "default", "b", "overridden"));
        assertExecutedWithParams(job2, Map.of("e", "default", "z", "added"));
        assertExecutedWithParams(job3, Map.of("i", "default", "k", "overridden", "y", "added"));
    }

    @Test
    public void testJobWithDependencies_1() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);

        List<ExecutableAtMostOnceJob> jobs = List.of(job3, job2, job1);

        testFactory.app("--exec", "--job=job1")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).config(e -> jobs.forEach(e::addJob)))
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.jobs.job1.dependsOn[0]", "job2")
                        .setProperty("bq.jobs.job2.dependsOn[0]", "job3"))
                .run();

        assertExecutedInOrder(jobs);
    }

    @Test
    public void testJobWithDependencies_2() {
        Job2 job2 = new Job2();
        Job3 job3 = new Job3(1000);

        List<ExecutableAtMostOnceJob> jobs = List.of(job3, job2);

        testFactory.app("--exec", "--job=job2")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).config(e -> jobs.forEach(e::addJob)))
                .module(b -> BQCoreModule.extend(b).setProperty("bq.jobs.job2.dependsOn[0]", "job3"))
                .run();

        assertExecutedInOrder(jobs);
    }

    @Test
    public void testGroup1_DefaultDependencies() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);

        List<ExecutableAtMostOnceJob> jobs = List.of(job3, job2, job1);

        testFactory.app("--exec", "--job=g1")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).config(e -> jobs.forEach(e::addJob)))
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.jobs.g1.type", "group")
                        .setProperty("bq.jobs.g1.jobs.job1.type", "single")
                        .setProperty("bq.jobs.job1.dependsOn[0]", "job2")
                        .setProperty("bq.jobs.job2.dependsOn[0]", "job3"))
                .run();

        assertExecutedInOrder(jobs);
    }

    @Test
    public void testGroup2_DefaultDependencies() {

        List<ExecutableAtMostOnceJob> jobs = List.of(new Job3(100000), new Job2(1000), new Job1());

        testFactory.app("--exec", "--job=g1")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).config(e -> jobs.forEach(e::addJob)))
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.jobs.g1.type", "group")
                        .setProperty("bq.jobs.g1.jobs.job1.type", "single")
                        .setProperty("bq.jobs.g1.jobs.job2.type", "single")
                        .setProperty("bq.jobs.g1.jobs.job3.type", "single")
                        .setProperty("bq.jobs.job1.dependsOn[0]", "job2")
                        .setProperty("bq.jobs.job2.dependsOn[0]", "job3"))
                .run();
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testGroup3_OverriddenDependencies() {

        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);

        List<ExecutableAtMostOnceJob> jobs = List.of(job1, job2, job3);
        testFactory.app("--exec", "--job=g1")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).config(e -> jobs.forEach(e::addJob)))
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.jobs.g1.type", "group")
                        .setProperty("bq.jobs.g1.jobs.job1.dependsOn[0]", "job3")
                        .setProperty("bq.jobs.job1.dependsOn[0]", "job2")
                        .setProperty("bq.jobs.job2.dependsOn[0]", "job3"))
                .run();

        assertExecutedInOrder(List.of(job3, job1));
    }

    @Test
    public void testGroup4_OverriddenDependencies() {
        List<ExecutableAtMostOnceJob> jobs = List.of(new Job2(), new Job1(), new Job3(1000));
        executeJobs(jobs, "--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=g1");
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testDefaultParameterValue() {
        ParameterizedJob1 p1 = new ParameterizedJob1();
        ParameterizedJob2 p2 = new ParameterizedJob2();

        testFactory.app("--config=classpath:io/bootique/job/config_parameters_conversion.yml",
                        "--exec",
                        "--job=parameterizedjob1")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(p1).addJob(p2))
                .run();

        p1.assertExecuted(Map.of("longp", 777L));
    }

    @Test
    public void testParametersOverriddenWithProps() {
        ParameterizedJob2 job = new ParameterizedJob2();

        testFactory.app("--exec", "--job=parameterizedjob2")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job))
                .module(b -> BQCoreModule.extend(b).setProperty("bq.jobs.parameterizedjob2.params.longp", "35"))
                .run();

        job.assertExecuted(Map.of("longp", 35L));
    }

    @Test
    public void testParametersOverriddenWithVars() {
        ParameterizedJob2 job = new ParameterizedJob2();

        testFactory.app("--exec", "--job=parameterizedjob2")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job))
                .module(b -> BQCoreModule.extend(b).declareVar("jobs.parameterizedjob2.params.longp", "TEST_PARAM"))
                .module(b -> BQCoreModule.extend(b).setVar("TEST_PARAM", "35"))
                .run();

        job.assertExecuted(Map.of("longp", 35L));
    }

    @Test
    public void testParam_Default() {
        ParameterizedJob4 job = new ParameterizedJob4();

        testFactory.app("--exec", "--job=parameterizedjob4")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job))
                .run();

        assertEquals(Map.of("xp", "_default_"), job.getParams());
    }

    @Test
    public void testParam_Default_Null() {
        ParameterizedJob5 job = new ParameterizedJob5();

        testFactory.app("--exec", "--job=parameterizedjob5")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job))
                .run();

        assertEquals(Collections.singletonMap("xp", null), job.getParams());
    }

    @Test
    public void testUnregisteredJob() {
        BQRuntime app = testFactory.app("--exec", "--job=dummy", "--config=classpath:io/bootique/job/config_dummy.yml")
                .autoLoadModules()
                .createRuntime();

        try {
            app.run();
            fail("Exception expected - no such job");
        } catch (DIRuntimeException e) {
            assertTrue(e.getCause() instanceof BootiqueException);
        }
    }

    @Test
    public void testGroup1_ParametersConversion() {
        ParameterizedJob1 p1 = new ParameterizedJob1();
        ParameterizedJob2 p2 = new ParameterizedJob2();

        testFactory.app("--config=classpath:io/bootique/job/config_parameters_conversion.yml",
                        "--exec",
                        "--job=group1")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(p1).addJob(p2))
                .run();

        p1.assertExecuted(Map.of("longp", 1L));
    }

    @Test
    public void testGroup2_ParametersConversion() {
        ParameterizedJob1 job1 = new ParameterizedJob1();
        ParameterizedJob2 job2 = new ParameterizedJob2();

        testFactory.app("--config=classpath:io/bootique/job/config_parameters_conversion.yml",
                        "--exec",
                        "--job=group2")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job1).addJob(job2))
                .run();

        job1.assertExecuted(Map.of("longp", 777L));
        job2.assertExecuted(Map.of("longp", 33L));
        job2.assertExecutedBefore(job1);
    }
}
