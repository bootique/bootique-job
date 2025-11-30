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

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.job.*;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class Scheduler_NoDeadlockIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler_NoDeadlockIT.class);

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b)
                    .setProperty("bq.scheduler.threadPoolSize", "2")
                    .setProperty("bq.scheduler.graphExecutorThreadPoolSize", "2")
                    .setProperty("bq.jobs.j1.dependsOn[0]", "j2")
                    .setProperty("bq.jobs.j1.dependsOn[1]", "j3")
                    .setProperty("bq.jobs.j1.dependsOn[2]", "j4")
                    .setProperty("bq.jobs.j1.dependsOn[3]", "j5")
            )
            .module(b -> JobsModule.extend(b)
                    .addJob(J1.class)
                    .addJob(J2.class)
                    .addJob(J3.class)
                    .addJob(J4.class)
                    .addJob(J5.class))
            .createRuntime();

    @Timeout(5)
    @RepeatedTest(3)
    public void attemptDeadlock_Get() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<JobFuture> futures = List.of(
                scheduler.newExecution().jobName("j1").runNonBlocking(),
                scheduler.newExecution().jobName("j1").runNonBlocking(),
                scheduler.newExecution().jobName("j1").runNonBlocking(),
                scheduler.newExecution().jobName("j1").runNonBlocking());

        for (JobFuture f : futures) {
            JobOutcome r = f.get();
            assertEquals(JobStatus.SUCCESS, r.getStatus());
        }
    }

    @Timeout(5)
    @RepeatedTest(3)
    public void attemptDeadlock_GetWithTimeout() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<JobFuture> futures = List.of(
                scheduler.newExecution().jobName("j1").runNonBlocking(),
                scheduler.newExecution().jobName("j1").runNonBlocking());

        for (JobFuture f : futures) {
            JobOutcome r = f.get(1, TimeUnit.SECONDS);
            assertEquals(JobStatus.SUCCESS, r.getStatus());
        }
    }

    static final class J1 extends BaseJob {

        public J1() {
            super(JobMetadata.build(J1.class));
        }

        @Override
        public JobOutcome run(Map<String, Object> params) {
            LOGGER.info("1 in progress");
            return JobOutcome.succeeded();
        }
    }

    static final class J2 extends BaseJob {

        public J2() {
            super(JobMetadata.build(J2.class));
        }

        @Override
        public JobOutcome run(Map<String, Object> params) {
            LOGGER.info("2 in progress");
            return JobOutcome.succeeded();
        }
    }

    static final class J3 extends BaseJob {

        public J3() {
            super(JobMetadata.build(J3.class));
        }

        @Override
        public JobOutcome run(Map<String, Object> params) {
            LOGGER.info("3 in progress");
            return JobOutcome.succeeded();
        }
    }

    static final class J4 extends BaseJob {

        public J4() {
            super(JobMetadata.build(J4.class));
        }

        @Override
        public JobOutcome run(Map<String, Object> params) {
            LOGGER.info("4 in progress");
            return JobOutcome.succeeded();
        }
    }

    static final class J5 extends BaseJob {

        public J5() {
            super(JobMetadata.build(J5.class));
        }

        @Override
        public JobOutcome run(Map<String, Object> params) {
            LOGGER.info("5 in progress");
            return JobOutcome.succeeded();
        }
    }
}
