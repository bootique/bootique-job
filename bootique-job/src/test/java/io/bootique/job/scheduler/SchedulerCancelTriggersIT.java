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
public class SchedulerCancelTriggersIT {

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app("-c", "classpath:io/bootique/job/scheduler/scheduler_schedule_triggers.yml")
            .modules(new JobsModule(), new SchedulerModule())
            .module(b -> JobsModule.extend(b).addJob(J1.class).addJob(J2.class))
            .createRuntime();

    @Test
    public void cancelAllTriggers() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertEquals(3, scheduler.scheduleAllTriggers());
        assertEquals(3, scheduler.cancelAllTriggers(false));
    }

    @Test
    public void cancelAllTriggers_repeat() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertEquals(3, scheduler.scheduleAllTriggers());
        assertEquals(3, scheduler.cancelAllTriggers(false));
        assertEquals(0, scheduler.cancelAllTriggers(false));
    }

    @Test
    public void cancelTriggers() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertEquals(3, scheduler.scheduleAllTriggers());
        assertEquals(2, scheduler.cancelTriggers("j2", false));
    }

    @Test
    public void cancelTriggers_UnknownJob() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        try {
            scheduler.cancelTriggers("no-such-job", false);
            fail("Exception expected on invalid job name");
        } catch (BootiqueException e) {
            assertEquals("Unknown job: no-such-job", e.getMessage());
        }
    }

    @Test
    public void cancelTriggers_repeat() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertEquals(3, scheduler.scheduleAllTriggers());
        assertEquals(2, scheduler.cancelTriggers("j2", false));
        assertEquals(0, scheduler.cancelTriggers("j2", false));
    }

    @Test
    public void cancelTrigger() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<Trigger> triggers = scheduler.getTriggers("j2");
        assertEquals(2, triggers.size());
        String triggerName = triggers.get(0).getTriggerName();

        assertTrue(scheduler.scheduleTrigger("j2", triggerName));
        assertTrue(triggers.get(0).isScheduled());

        assertTrue(scheduler.cancelTrigger("j2", triggerName, false));
        assertTrue(triggers.get(0).isCanceled());

        assertFalse(scheduler.cancelTrigger("j2", triggerName, false));
    }

    @Test
    public void cancelTrigger_UnknownTrigger() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        try {
            scheduler.cancelTrigger("j1", "no-such-trigger", false);
            fail("Exception expected on invalid trigger name");
        } catch (IllegalArgumentException e) {
            assertEquals("No such trigger: j1:no-such-trigger", e.getMessage());
        }
    }

    @Test
    public void cancelTrigger_mayInterruptIfRunning_true() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<Trigger> triggers = scheduler.getTriggers("j1");
        assertEquals(1, triggers.size());
        String triggerName = triggers.get(0).getTriggerName();

        assertTrue(scheduler.scheduleTrigger("j1", triggerName));

        assertTrue(scheduler.cancelTrigger("j1", triggerName, true));
        assertTrue(triggers.get(0).isCanceled());
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
