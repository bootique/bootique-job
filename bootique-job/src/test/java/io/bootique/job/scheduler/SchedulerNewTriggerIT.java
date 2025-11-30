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
import io.bootique.job.Job;
import io.bootique.job.JobOutcome;
import io.bootique.job.JobsModule;
import io.bootique.job.Scheduler;
import io.bootique.job.SchedulerModule;
import io.bootique.job.trigger.CronTrigger;
import io.bootique.job.trigger.FixedDelayTrigger;
import io.bootique.job.trigger.FixedRateTrigger;
import io.bootique.job.trigger.Trigger;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class SchedulerNewTriggerIT {

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .modules(new JobsModule(), new SchedulerModule())
            .module(b -> JobsModule.extend(b).addJob(J1.class).addJob(J2.class))
            .createRuntime();

    @Test
    public void newCronTrigger() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertTrue(scheduler.getAllTriggers().isEmpty());

        Trigger t = scheduler.newCronTrigger("* * * * * ?").jobName("j2").triggerName("t1").add();
        assertNotNull(t);
        assertTrue(t.isUnscheduled());
        assertEquals(1, scheduler.getAllTriggers().size());
        assertInstanceOf(CronTrigger.class, t);
        assertSame(t, scheduler.getTrigger("j2", "t1"));
    }

    @Test
    public void newFixedRateTrigger() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertTrue(scheduler.getAllTriggers().isEmpty());

        Trigger t = scheduler.newFixedRateTrigger(Duration.ofSeconds(5), Duration.ofSeconds(1)).jobName("j1").triggerName("t2").add();
        assertNotNull(t);
        assertTrue(t.isUnscheduled());
        assertEquals(1, scheduler.getAllTriggers().size());
        assertInstanceOf(FixedRateTrigger.class, t);
        assertSame(t, scheduler.getTrigger("j1", "t2"));
    }

    @Test
    public void newFixedDelayTrigger() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        assertTrue(scheduler.getAllTriggers().isEmpty());

        Trigger t = scheduler.newFixedDelayTrigger(Duration.ofSeconds(3), Duration.ofSeconds(2)).jobName("j1").triggerName("t3").add();
        assertNotNull(t);
        assertTrue(t.isUnscheduled());
        assertEquals(1, scheduler.getAllTriggers().size());
        assertInstanceOf(FixedDelayTrigger.class, t);
        assertSame(t, scheduler.getTrigger("j1", "t3"));
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
