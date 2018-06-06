/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.job;

import io.bootique.BQCoreModule;
import io.bootique.BootiqueException;
import io.bootique.job.fixture.ExecutableAtMostOnceJob;
import io.bootique.job.fixture.Job1;
import io.bootique.job.fixture.Job2;
import io.bootique.job.fixture.Job3;
import io.bootique.job.fixture.ParameterizedJob1;
import io.bootique.job.fixture.ParameterizedJob2;
import io.bootique.job.runtime.JobModule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ExecutionIT extends BaseJobExecIT {

    @Test
    public void testExecution_SingleJob_DefaultParams() {
        Job1 job1 = new Job1();
        String[] args = new String[]{"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=job1"};

        List<ExecutableAtMostOnceJob> jobs = Collections.singletonList(job1);
        executeJobs(jobs, args);
        assertExecuted(jobs);
    }

    @Test
    public void testExecution_Group1_SingleJob_DefaultParams() {
        Job1 job1 = new Job1();
        String[] args = new String[]{"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group1"};

        List<ExecutableAtMostOnceJob> jobs = Collections.singletonList(job1);
        executeJobs(jobs, args);
        assertExecuted(jobs);
    }

    @Test
    public void testExecution_Group6_SingleJob_OverridenParams() {
        Job1 job1 = new Job1();
        String[] args = new String[]{"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group6"};

        List<ExecutableAtMostOnceJob> jobs = Collections.singletonList(job1);
        executeJobs(jobs, args);
        assertExecutedWithParams(job1, new HashMap<String, Object>() {{
            put("a", "overriden");
            put("b", "default");
        }});
    }

    @Test
    public void testExecution_Group2_MultipleJobs_Parallel_DefaultParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2();
        String[] args = new String[]{"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group2"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job1, job2);
        executeJobs(jobs, args);
        assertExecuted(jobs);
    }

    @Test
    public void testExecution_Group3_MultipleJobs_Parallel_OverridenParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2();
        String[] args = new String[]{"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group3"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job1, job2);
        executeJobs(jobs, args);
        assertExecutedWithParams(job1, new HashMap<String, Object>() {{
            put("a", "overriden");
            put("b", "default");
        }});
        assertExecutedWithParams(job2, new HashMap<String, Object>() {{
            put("e", "default");
            put("y", "added");
        }});
    }

    @Test
    public void testExecution_Group4_MultipleJobs_Dependent_OverridenParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        String[] args = new String[]{"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group4"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
        assertExecutedWithParams(job2, Collections.singletonMap("e", "overriden"));
    }

    @Test
    public void testExecution_Group5_MultipleJobs_Dependent_OverridenParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);
        String[] args = new String[]{"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group5"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job3, job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
        assertExecutedWithParams(job1, new HashMap<String, Object>() {{
            put("a", "default");
            put("b", "overriden");
        }});
        assertExecutedWithParams(job2, new HashMap<String, Object>() {{
            put("e", "default");
            put("z", "added");
        }});
        assertExecutedWithParams(job3, new HashMap<String, Object>() {{
            put("i", "default");
            put("k", "overriden");
            put("y", "added");
        }});
    }

    @Test
    public void testExecution_JobWithDependencies_1() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);
        String[] args = new String[]{"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=job1"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job3, job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_JobWithDependencies_2() {
        Job2 job2 = new Job2();
        Job3 job3 = new Job3(1000);
        String[] args = new String[]{"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=job2"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job3, job2);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_Group1_DefaultDependencies() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);
        String[] args = new String[]{"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=group1"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job3, job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_Group2_DefaultDependencies() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);
        String[] args = new String[]{"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=group2"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job3, job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_Group3_OverridenDependencies() {
        Job1 job1 = new Job1();
        Job3 job3 = new Job3(1000);
        String[] args = new String[]{"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=group3"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job3, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_Group4_OverridenDependencies() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2();
        Job3 job3 = new Job3(1000);
        String[] args = new String[]{"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=group4"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job2, job1, job3);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_ParameterizedJob1_DefaultParameterValue() {
        ParameterizedJob1 job1 = new ParameterizedJob1();
        String[] args = new String[]{"--config=classpath:io/bootique/job/config_parameters_conversion.yml",
                "--exec", "--job=parameterizedjob1"};

        executeJobs(Collections.singleton(job1), args);
        assertExecutedWithParams(job1, Collections.singletonMap("longp", 777L));
    }

    @Test
    public void testExecution_ParameterizedJob1_ParametersOverriddenWithProps() {
        ParameterizedJob2 job = new ParameterizedJob2();

        testFactory.app("--exec", "--job=parameterizedjob2")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job))
                .module(b -> BQCoreModule.extend(b).setProperty("bq.jobs.parameterizedjob2.params.longp", "35"))
                .createRuntime()
                .run();

        assertExecutedWithParams(job, Collections.singletonMap("longp", 35l));
    }

    @Test
    public void testExecution_ParameterizedJob1_ParametersOverriddenWithVars() {
        ParameterizedJob2 job = new ParameterizedJob2();

        testFactory.app("--exec", "--job=parameterizedjob2")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job))
                .module(b -> BQCoreModule.extend(b).declareVar("jobs.parameterizedjob2.params.longp", "TEST_PARAM"))
                .module(b -> BQCoreModule.extend(b).setVar("TEST_PARAM", "35"))
                .createRuntime()
                .run();

        assertExecutedWithParams(job, Collections.singletonMap("longp", 35l));
    }

    @Test(expected = BootiqueException.class)
    public void testExecution_UnregisteredJob() {
        testFactory.app("--exec", "--job=dummy",
                "--config=classpath:io/bootique/job/config_dummy.yml")
                .autoLoadModules()
                .createRuntime()
                .run();
    }

    @Test
    public void testExecution_Group1_ParametersConversion() {
        ParameterizedJob1 job1 = new ParameterizedJob1();
        String[] args = new String[]{"--config=classpath:io/bootique/job/config_parameters_conversion.yml",
                "--exec", "--job=group1"};

        executeJobs(Collections.singleton(job1), args);
        assertExecutedWithParams(job1, Collections.singletonMap("longp", 1L));
    }

    @Test
    public void testExecution_Group2_ParametersConversion() {
        ParameterizedJob1 job1 = new ParameterizedJob1();
        ParameterizedJob2 job2 = new ParameterizedJob2();
        String[] args = new String[]{"--config=classpath:io/bootique/job/config_parameters_conversion.yml",
                "--exec", "--job=group2"};

        List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
        assertExecutedWithParams(job1, Collections.singletonMap("longp", 777L));
        assertExecutedWithParams(job2, Collections.singletonMap("longp", 33L));
    }
}
