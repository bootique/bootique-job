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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.BootiqueException;
import io.bootique.job.Job;
import io.bootique.job.JobOutcome;
import io.bootique.job.JobsModule;
import io.bootique.job.Scheduler;
import io.bootique.job.SchedulerModule;
import io.bootique.job.trigger.Trigger;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


@BQTest
public class SchedulerScheduleTriggersIT {

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app("-c", "classpath:io/bootique/job/scheduler/scheduler_schedule_triggers.yml")
            .modules(new JobsModule(), new SchedulerModule())
            .module(b -> JobsModule.extend(b).addJob(J1.class).addJob(J2.class))
            .createRuntime();

    @Test
    public void scheduleAllTriggers() {
        Scheduler scheduler = app.getInstance(Scheduler.class);
        assertEquals(3, scheduler.scheduleAllTriggers());
    }

    @Test
    public void scheduleAllTriggers_repeat() {
        Scheduler scheduler = app.getInstance(Scheduler.class);
        assertEquals(3, scheduler.scheduleAllTriggers());
        assertEquals(0, scheduler.scheduleAllTriggers());
    }

    @Test
    public void scheduleTriggers_UnknownJob() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        try {
            scheduler.scheduleTriggers("no-such-job");
            fail("Exception excepted on invalid job name");
        } catch (BootiqueException e) {
            assertEquals("Unknown job: no-such-job", e.getMessage());
        }
    }

    @Test
    public void scheduleTriggers_repeat() {
        Scheduler scheduler = app.getInstance(Scheduler.class);
        assertEquals(2, scheduler.scheduleTriggers("j2"));
        assertEquals(0, scheduler.scheduleTriggers("j2"));
    }

    @Test
    public void scheduleTrigger() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<Trigger> triggers = scheduler.getTriggers("j2");
        assertEquals(2, triggers.size());
        assertTrue(triggers.getFirst().isUnscheduled());
        String triggerName = triggers.getFirst().getTriggerName();

        assertTrue(scheduler.scheduleTrigger("j2", triggerName));
        assertTrue(triggers.getFirst().isScheduled());

        assertFalse(scheduler.scheduleTrigger("j2", triggerName));
    }

    @Test
    public void scheduleTrigger_UnknownTrigger() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        try {
            scheduler.scheduleTrigger("j1", "no-such-trigger");
            fail("Exception expected on invalid trigger name");
        } catch (IllegalArgumentException e) {
            assertEquals("No such trigger: j1:no-such-trigger", e.getMessage());
        }
    }

    static class J1 implements Job {
        @Override
        public JobOutcome run(Map<String, Object> params) {
            return JobOutcome.succeeded();
        }
    }

    static class J2 implements Job {
        @Override
        public JobOutcome run(Map<String, Object> params) {
            return JobOutcome.succeeded();
        }
    }
}
