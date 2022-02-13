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

import io.bootique.command.CommandOutcome;
import io.bootique.job.Job;
import io.bootique.job.JobListener;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runtime.JobModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class SchedulerParamsIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testJobParams() {
        new ParamsTester(testFactory)
                .param("a", "b")
                .run("classpath:io/bootique/job/scheduler/config-job-params.yml")
                .waitAndAssertParameters();
    }

    @Test
    public void testJobGroupParamsOverrideJobParams() {
        new ParamsTester(testFactory)
                .param("a", "b")
                .param("b", "d")
                .run("classpath:io/bootique/job/scheduler/config-job-group-params.yml")
                .waitAndAssertParameters();
    }

    @Test
    public void testJobTriggerParamsOverrideGroupAndJobParams() {
        new ParamsTester(testFactory)
                .param("a", "b")
                .param("b", "e")
                .param("c", "g")
                .run("classpath:io/bootique/job/scheduler/config-trigger-params.yml")
                .waitAndAssertParameters();
    }

    public static class J1 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build("j1");
        }

        @Override
        public JobResult run(Map<String, Object> parameters) {
            return JobResult.success(getMetadata());
        }
    }

    public static class ParamsTester implements JobListener {

        private BQTestFactory testFactory;
        private Map<String, Object> expectedParams;
        private Map<String, Object> params;
        private JobResult result;

        ParamsTester(BQTestFactory testFactory) {
            this.testFactory = testFactory;
            this.expectedParams = new HashMap<>();
            this.params = Collections.emptyMap();
        }

        ParamsTester param(String key, Object value) {
            expectedParams.put(key, value);
            return this;
        }

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry) {
            this.params = new HashMap<>(parameters);
            onFinishedCallbackRegistry.accept(jr -> this.result = jr);
        }

        public ParamsTester run(String config) {
            CommandOutcome outcome = testFactory.app("--schedule", "-c", config)
                    .module(b -> JobModule.extend(b).addJob(J1.class).addListener(this))
                    .run();

            assertTrue(outcome.isSuccess());
            return this;
        }

        public void waitAndAssertParameters() {

            for (int i = 0; i < 10; i++) {
                if (isFinished()) {
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    fail("Awoken");
                }
            }

            assertTrue(isFinished(), "Job failed to run");
            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(expectedParams, params);
        }

        private boolean isFinished() {
            return result != null;
        }
    }
}
