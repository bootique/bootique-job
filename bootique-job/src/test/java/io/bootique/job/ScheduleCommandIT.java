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
import io.bootique.job.fixture.ScheduledJob1;
import io.bootique.job.fixture.ScheduledJob2;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class ScheduleCommandIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testScheduleCommand_AllJobs() {
        BQRuntime runtime = testFactory.app()
                .args("--schedule", "-c", "classpath:io/bootique/job/fixture/scheduler_test_command.yml")
                .module(JobModule.class)
                .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addJob(ScheduledJob2.class))
                .createRuntime();

        Scheduler scheduler = runtime.getInstance(Scheduler.class);
        assertFalse(scheduler.isStarted());

        runtime.run();

        assertTrue(scheduler.isStarted());
        assertEquals(2, scheduler.getScheduledJobs().size());
    }

    @Test
    public void testScheduleCommand_SelectedJobs() {
        BQRuntime runtime = testFactory.app()
                .args("--schedule", "--job=scheduledjob1", "-c", "classpath:io/bootique/job/fixture/scheduler_test_triggers.yml")
                .module(JobModule.class)
                .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class).addJob(ScheduledJob2.class))
                .createRuntime();

        Scheduler scheduler = runtime.getInstance(Scheduler.class);

        runtime.run();

        assertTrue(scheduler.isStarted());
        assertEquals(1, scheduler.getScheduledJobs().size());
        assertEquals("scheduledjob1", scheduler.getScheduledJobs().iterator().next().getJobName());
    }
}
