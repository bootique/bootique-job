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
import io.bootique.Bootique;
import io.bootique.command.CommandOutcome;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class ExecCommand_DeepGroups {

    static final Logger LOGGER = LoggerFactory.getLogger(ExecCommand_DeepGroups.class);

    static final ConcurrentMap<String, Integer> results = new ConcurrentHashMap<>();

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique.app(
                    "--exec",
                    "--job=group")
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b)
                    .setProperty("bq.jobs.group.type", "group")
                    .setProperty("bq.jobs.group.jobs.j1.dependsOn[0]", "j2")
                    .setProperty("bq.jobs.group.jobs.j1.dependsOn[1]", "j3")
                    .setProperty("bq.jobs.group.jobs.j1.dependsOn[2]", "j4")
                    .setProperty("bq.jobs.j4.dependsOn[0]", "j5")
                    .setProperty("bq.jobs.j4.dependsOn[1]", "j6"))
            .module(b -> JobsModule.extend(b)
                    .addJob(J1.class).addJob(J2.class)
                    .addJob(J3.class).addJob(J4.class)
                    .addJob(J5.class).addJob(J6.class))
            .createRuntime();

    @BeforeEach
    void resetResults() {
        results.clear();
    }

    @Test
    public void exec() {
        CommandOutcome result = app.run();
        assertTrue(result.isSuccess());

        // TODO: smarter assertions reflecting job dependency topology and threads used for execution
        assertEquals(1, results.get("j1"));
        assertEquals(1, results.get("j2"));
        assertEquals(1, results.get("j3"));
        assertEquals(1, results.get("j4"));
        assertEquals(1, results.get("j5"));
        assertEquals(1, results.get("j6"));
    }

    static class J1 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J1.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            LOGGER.info("running 1");
            results.put("j1", 1);
            return JobResult.succeeded();
        }
    }

    static class J2 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J2.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            LOGGER.info("running 2");
            results.put("j2", 1);
            return JobResult.succeeded();
        }
    }

    static class J3 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J3.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            LOGGER.info("running 3");
            results.put("j3", 1);
            return JobResult.succeeded();
        }
    }

    static class J4 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J4.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            LOGGER.info("running 4");
            results.put("j4", 1);
            return JobResult.succeeded();
        }
    }

    static class J5 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J5.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            LOGGER.info("running 5");
            results.put("j5", 1);
            return JobResult.succeeded();
        }
    }

    static class J6 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J6.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            LOGGER.info("running 6");
            results.put("j6", 1);
            return JobResult.succeeded();
        }
    }
}
