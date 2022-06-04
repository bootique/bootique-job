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

import io.bootique.BQRuntime;
import io.bootique.job.runnable.JobResult;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class JobRegistryIT {

    @BQTestTool
    final static BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testAddJob() {

        BQRuntime runtime = testFactory.app()
                .module(b -> JobModule.extend(b).addJob(J1.class).addJob(J2.class))
                .createRuntime();

        JobRegistry registry = runtime.getInstance(JobRegistry.class);
        assertEquals(new HashSet<>(asList("j1", "j2")), registry.getJobNames());
        assertDoesNotThrow(() -> registry.getJob("j1"));
        assertDoesNotThrow(() -> registry.getJob("j2"));
    }

    @Test
    public void testAddJob_Duplicate() {

        BQRuntime runtime = testFactory.app()
                .module(b -> JobModule.extend(b)
                        .addJob(J1.class)
                        .addJob(J2.class)
                        .addJob(J1.class)
                        .addJob(J2.class))
                .createRuntime();

        JobRegistry registry = runtime.getInstance(JobRegistry.class);
        assertEquals(new HashSet<>(asList("j1", "j2")), registry.getJobNames());
        assertDoesNotThrow(() -> registry.getJob("j1"));
        assertDoesNotThrow(() -> registry.getJob("j2"));
    }

    static class J1 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J1.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            return JobResult.success(getMetadata());
        }
    }

    static class J2 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J2.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            return JobResult.success(getMetadata());
        }
    }
}
