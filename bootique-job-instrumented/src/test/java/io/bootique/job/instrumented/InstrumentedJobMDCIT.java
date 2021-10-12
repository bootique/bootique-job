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
import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.logback.LogbackModule;
import io.bootique.metrics.MetricsModule;
import io.bootique.metrics.mdc.TransactionIdMDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class InstrumentedJobMDCIT {

    @BQTestTool
    final BQTestFactory factory = new BQTestFactory();

    @BeforeEach
    void resetJobs() {
        ScheduledJob1.counter.set(0);
        ScheduledJob1.tx.clear();

        ScheduledJob2.counter.set(0);
        ScheduledJob2.tx.clear();
    }

    @Test
    public void testScheduleJob() throws InterruptedException {
        BQRuntime app = factory.app("-c", "classpath:io/bootique/job/instrumented/schedule.yml", "--schedule")
                .module(new LogbackModule())
                .module(new MetricsModule())
                .module(new JobModule())
                .module(new JobInstrumentedModule())
                .module(binder -> JobModule.extend(binder).addJob(ScheduledJob1.class).addJob(ScheduledJob2.class))
                .createRuntime();

        Scheduler scheduler = app.getInstance(Scheduler.class);
        TransactionIdMDC mdc = app.getInstance(TransactionIdMDC.class);

        app.run();

        // let the jobs run for a while and analyze TX ids
        Thread.sleep(1000L);
        app.shutdown();

        int j1c = ScheduledJob1.counter.get();
        assertTrue(j1c > 0);
        Set<String> uniqueIds1 = new HashSet<>(ScheduledJob1.tx.values());
        assertEquals(j1c, uniqueIds1.size());

        int j2c = ScheduledJob2.counter.get();
        assertTrue(j2c > 0);
        Set<String> uniqueIds2 = new HashSet<>(ScheduledJob2.tx.values());
        assertEquals(j2c, uniqueIds2.size());

        Set<String> uniqueIds = new HashSet<>();
        uniqueIds.addAll(uniqueIds1);
        uniqueIds.addAll(uniqueIds2);
        assertEquals(j1c + j2c, uniqueIds.size());
    }

    static class ScheduledJob1 extends BaseJob {
        static Map<Integer, String> tx = new ConcurrentHashMap<>();
        static AtomicInteger counter = new AtomicInteger(0);

        public ScheduledJob1() {
            super(JobMetadata.build(ScheduledJob1.class));
        }

        @Override
        public JobResult run(Map<String, Object> params) {

            int next = counter.getAndIncrement();
            tx.put(next, MDC.get(TransactionIdMDC.MDC_KEY));

            return JobResult.success(getMetadata());
        }
    }


    static class ScheduledJob2 extends BaseJob {

        static Map<Integer, String> tx = new ConcurrentHashMap<>();
        static AtomicInteger counter = new AtomicInteger(0);

        public ScheduledJob2() {
            super(JobMetadata.build(ScheduledJob2.class));
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            int next = counter.getAndIncrement();
            tx.put(next, MDC.get(TransactionIdMDC.MDC_KEY));
            return JobResult.success(getMetadata());
        }
    }
}
