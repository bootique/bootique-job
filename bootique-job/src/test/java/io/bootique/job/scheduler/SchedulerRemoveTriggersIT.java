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
public class SchedulerRemoveTriggersIT {

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app("-c", "classpath:io/bootique/job/scheduler/scheduler_schedule_triggers.yml")
            .modules(new JobsModule(), new SchedulerModule())
            .module(b -> JobsModule.extend(b).addJob(J1.class).addJob(J2.class))
            .createRuntime();

    @Test
    public void removeAllTriggers() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertEquals(3, scheduler.getAllTriggers().size());
        assertEquals(3, scheduler.removeAllTriggers());
        assertEquals(0, scheduler.getAllTriggers().size());
    }

    @Test
    public void removeAllTriggers_repeat() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertEquals(3, scheduler.removeAllTriggers());
        assertEquals(0, scheduler.removeAllTriggers());
    }

    @Test
    public void removeAllTriggers_scheduled() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<Trigger> triggers = scheduler.getAllTriggers();
        assertEquals(3, triggers.size());

        assertEquals(3, scheduler.scheduleAllTriggers());
        assertTrue(triggers.getFirst().isScheduled());

        assertEquals(3, scheduler.removeAllTriggers());
        assertTrue(triggers.getFirst().isCanceled());
        assertEquals(0, scheduler.getAllTriggers().size());
    }

    @Test
    public void removeTriggers() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertEquals(3, scheduler.getAllTriggers().size());
        assertEquals(2, scheduler.removeTriggers("j2"));
        assertEquals(1, scheduler.getAllTriggers().size());
    }

    @Test
    public void removeTriggers_UnknownJob() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        try {
            scheduler.removeTriggers("no-such-job");
            fail("Exception expected on invalid job name");
        } catch (BootiqueException e) {
            assertEquals("Unknown job: no-such-job", e.getMessage());
        }
    }

    @Test
    public void removeTriggers_repeat() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertEquals(2, scheduler.removeTriggers("j2"));
        assertEquals(0, scheduler.removeTriggers("j2"));
    }

    @Test
    public void removeTriggers_scheduled() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<Trigger> triggersJ2 = scheduler.getTriggers("j2");
        assertEquals(2, triggersJ2.size());

        assertEquals(2, scheduler.scheduleTriggers("j2"));
        assertTrue(triggersJ2.getFirst().isScheduled());

        assertEquals(2, scheduler.removeTriggers("j2"));
        assertTrue(triggersJ2.getFirst().isCanceled());
        assertEquals(0, scheduler.getTriggers("j2").size());
    }

    @Test
    public void removeTrigger() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<Trigger> triggers = scheduler.getTriggers("j2");
        assertEquals(2, triggers.size());
        String triggerName = triggers.getFirst().getTriggerName();

        assertTrue(scheduler.removeTrigger("j2", triggerName));
        assertEquals(1, scheduler.getTriggers("j2").size());
    }

    @Test
    public void removeTrigger_repeat() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<Trigger> triggers = scheduler.getTriggers("j2");
        String triggerName = triggers.getFirst().getTriggerName();

        assertTrue(scheduler.removeTrigger("j2", triggerName));
        assertFalse(scheduler.removeTrigger("j2", triggerName));
    }

    @Test
    public void removeTrigger_scheduled() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        List<Trigger> triggers = scheduler.getTriggers("j2");
        assertEquals(2, triggers.size());
        String triggerName = triggers.getFirst().getTriggerName();

        assertTrue(scheduler.scheduleTrigger("j2", triggerName));
        assertTrue(triggers.getFirst().isScheduled());

        assertTrue(scheduler.removeTrigger("j2", triggerName));
        assertTrue(triggers.getFirst().isCanceled());
        assertEquals(1, scheduler.getTriggers("j2").size());
    }

    @Test
    public void removeTrigger_UnknownJob() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertFalse(scheduler.removeTrigger("no-such-job", "no-such-trigger"));
    }

    @Test
    public void removeTrigger_UnknownTrigger() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertFalse(scheduler.removeTrigger("j1", "no-such-trigger"));
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
