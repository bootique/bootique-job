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
import io.bootique.job.*;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.metrics.mdc.TransactionIdMDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class InstrumentedJobMDCIT {

    static final String NULL_PLACEHOLDER = "___";

    @BQTestTool
    final BQTestFactory factory = new BQTestFactory();

    @BeforeEach
    void resetJobs() {
        Job1.counter.set(0);
        Job1.tx.clear();

        Job2.counter.set(0);
        Job2.tx.clear();
    }

    @Test
    public void twoJobs() throws InterruptedException {
        BQRuntime app = factory.app("-c", "classpath:io/bootique/job/instrumented/InstrumentedJobMDCIT.yml", "--schedule")
                .autoLoadModules()
                .module(binder -> JobsModule.extend(binder)
                        .addJob(Job1.class)
                        .addJob(Job2.class))
                .createRuntime();

        // let the jobs run for a while and analyze TX ids
        app.run();
        Thread.sleep(1000L);
        app.shutdown();

        int j1c = Job1.counter.get();
        assertTrue(j1c > 0);
        Set<String> uniqueIds1 = new HashSet<>(Job1.tx.values());
        assertEquals(j1c, uniqueIds1.size());

        int j2c = Job2.counter.get();
        assertTrue(j2c > 0);
        Set<String> uniqueIds2 = new HashSet<>(Job2.tx.values());
        assertEquals(j2c, uniqueIds2.size());

        Set<String> uniqueIds = new HashSet<>();
        uniqueIds.addAll(uniqueIds1);
        uniqueIds.addAll(uniqueIds2);
        assertEquals(j1c + j2c, uniqueIds.size());
    }

    @Test
    public void jobGroupAndJob() throws InterruptedException {
        BQRuntime app = factory.app("-c", "classpath:io/bootique/job/instrumented/InstrumentedJobMDCIT-groups.yml", "--schedule")
                .autoLoadModules()
                .module(binder -> JobsModule.extend(binder).addJob(Job1.class).addJob(Job2.class).addJob(Job3.class).addJob(Job4.class))
                .createRuntime();

        // Running one job directly, and another - in a group. MDC is available to the direct job and must not be
        // reused

        // let the jobs run for a while and analyze TX ids
        app.run();
        Thread.sleep(1000L);
        app.shutdown();

        int j1c = Job1.counter.get();
        assertTrue(j1c > 0);
        Set<String> uniqueIds1 = new HashSet<>(Job1.tx.values());
        assertEquals(j1c, uniqueIds1.size());

        int j2c = Job2.counter.get();
        assertTrue(j2c > 0);
        Set<String> uniqueIds2 = new HashSet<>(Job2.tx.values());
        assertEquals(j2c, uniqueIds2.size());
        assertFalse(uniqueIds2.contains(NULL_PLACEHOLDER));
    }


    static class Job1 extends BaseJob {
        static final Logger LOGGER = LoggerFactory.getLogger(Job1.class);

        static final Map<Integer, String> tx = new ConcurrentHashMap<>();
        static final AtomicInteger counter = new AtomicInteger(0);

        public Job1() {
            super(JobMetadata.build(Job1.class));
        }

        @Override
        public JobResult run(Map<String, Object> params) {

            LOGGER.info("in job1");

            int next = counter.getAndIncrement();
            String id = MDC.get(TransactionIdMDC.MDC_KEY);
            tx.put(next, id != null ? id : NULL_PLACEHOLDER);
            return JobResult.success(getMetadata());
        }
    }

    static class Job2 extends BaseJob {
        static final Logger LOGGER = LoggerFactory.getLogger(Job2.class);

        static final Map<Integer, String> tx = new ConcurrentHashMap<>();
        static final AtomicInteger counter = new AtomicInteger(0);

        public Job2() {
            super(JobMetadata.build(Job2.class));
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            LOGGER.info("in job2");
            int next = counter.getAndIncrement();
            String id = MDC.get(TransactionIdMDC.MDC_KEY);
            tx.put(next, id != null ? id : NULL_PLACEHOLDER);
            return JobResult.success(getMetadata());
        }
    }

    static class Job3 extends BaseJob {

        public Job3() {
            super(JobMetadata.build(Job3.class));
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            return JobResult.success(getMetadata());
        }
    }

    static class Job4 extends BaseJob {

        public Job4() {
            super(JobMetadata.build(Job4.class));
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            return JobResult.success(getMetadata());
        }
    }
}
