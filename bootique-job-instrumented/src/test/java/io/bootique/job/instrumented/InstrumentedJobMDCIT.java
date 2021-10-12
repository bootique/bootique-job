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

package io.bootique.job.instrumented;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.scheduler.ScheduledJobFuture;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.junit5.BQTest;
import io.bootique.logback.LogbackModule;
import io.bootique.metrics.MetricsModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class InstrumentedJobMDCIT {

    final BQRuntime app = Bootique.app("-c", "classpath:io/bootique/job/instrumented/schedule.yml")
            .module(new LogbackModule())
            .module(new MetricsModule())
            .module(new JobModule())
            .module(new JobInstrumentedModule())
            .module(binder -> {
                JobModule.extend(binder)
                        .addJob(ScheduledJob1.class)
                        .addJob(ScheduledJob2.class);

            }).createRuntime();

    @AfterEach
    public void after() {
        getScheduler().getScheduledJobs().forEach(ScheduledJobFuture::cancelInterruptibly);
    }

    @Test
    public void testScheduleJob() throws InterruptedException {
        Scheduler scheduler = getScheduler();

        int jobCount = scheduler.start();
        assertEquals(2, jobCount);

        Collection<ScheduledJobFuture> scheduledJobs = scheduler.getScheduledJobs();
        assertEquals(2, scheduledJobs.size());

        // sleep here to let the jobs run and generate some logs
        // TODO: can we make any assertions here? Otherwise this test can only be checked visually
        Thread.sleep(1000);
    }

    private Scheduler getScheduler() {
        return app.getInstance(Scheduler.class);
    }
}
