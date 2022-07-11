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
package io.bootique.job.graph;

import io.bootique.BQCoreModule;
import io.bootique.job.*;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class JobGraphIT {

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testDependency() {

        JobRegistry registry = testFactory.app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(J1.class).addJob(J2.class).addJob(J3.class))
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.jobs.j1.dependsOn[0]", "j2")
                        .setProperty("bq.jobs.j2.dependsOn[0]", "j3"))
                .createRuntime()
                .getInstance(JobRegistry.class);

        Job j1 = registry.getJob("j1");
        assertTrue(j1.getMetadata().isGroup());
        assertEquals(Set.of("j2"), j1.getMetadata().getDependsOn());

        Job j2 = registry.getJob("j2");
        assertTrue(j2.getMetadata().isGroup());
        assertEquals(Set.of("j3"), j2.getMetadata().getDependsOn());

        Job j3 = registry.getJob("j3");
        assertFalse(j3.getMetadata().isGroup());
        assertEquals(Set.of(), j3.getMetadata().getDependsOn());
    }

    @Test
    public void testDependencyCycle() {

        JobRegistry registry = testFactory.app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(J1.class).addJob(J2.class).addJob(J3.class))
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.jobs.j1.dependsOn[0]", "j2")
                        .setProperty("bq.jobs.j2.dependsOn[0]", "j3")
                        .setProperty("bq.jobs.j3.dependsOn[0]", "j1"))
                .createRuntime()
                .getInstance(JobRegistry.class);

        try {
            registry.getJob("j1");
            fail("Job cycle was ignored");
        } catch (IllegalStateException e) {
            assertEquals("Job dependency cycle detected: [...] -> j3 -> j1", e.getMessage());
        }
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

    static class J3 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J3.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            return JobResult.success(getMetadata());
        }
    }
}
