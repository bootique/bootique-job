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
import io.bootique.di.Key;
import io.bootique.di.TypeLiteral;
import io.bootique.job.fixture.Job1;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runtime.JobModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class ListenerIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @BeforeEach
    public void before() {
        SharedState.reset();
    }

    @Test
    public void testAddMappedListener_Ordering1() {
        Job1 job = new Job1(0);

        testFactory.app("--exec", "--job=job1")
                .module(new JobModule())
                .module(binder -> JobModule.extend(binder)
                        .addJob(job)
                        .addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                        .addMappedListener(new MappedJobListener<>(new Listener2(), 2))
                        .addMappedListener(new MappedJobListener<>(new Listener3(), 3)))
                .run();

        assertTrue(job.isExecuted());
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void testAddMappedListener_Ordering2() {
        Job1 job = new Job1(0);

        testFactory.app("--exec", "--job=job1")
                .module(new JobModule())
                .module(binder -> JobModule.extend(binder)
                        .addJob(job)
                        .addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                        .addMappedListener(new MappedJobListener<>(new Listener3(), 3))
                        .addMappedListener(new MappedJobListener<>(new Listener2(), 2)))
                .run();

        assertTrue(job.isExecuted());
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void testAddMappedListener_AddListener_Ordering() {
        Job1 job = new Job1(0);

        testFactory.app("--exec", "--job=job1")
                .module(new JobModule())
                .module(binder -> JobModule.extend(binder).addJob(job)
                        .addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                        .addListener(new Listener2())
                        .addMappedListener(new MappedJobListener<>(new Listener3(), 2)))
                .run();

        assertTrue(job.isExecuted());
        assertEquals("_L1_started_L3_started_L2_started_L2_finished_L3_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void testStandardMappedListeners() {
        Job1 job = new Job1(0);

        BQRuntime runtime = testFactory.app()
                .module(new JobModule())
                .module(binder ->
                        JobModule.extend(binder)
                                .addJob(job)
                                .addListener(new Listener3())
                                .addMappedListener(new MappedJobListener<>(new Listener2(), 1)))
                .createRuntime();


        Set<MappedJobListener> mappedListeners = runtime.getInstance(Key.get(new TypeLiteral<Set<MappedJobListener>>() {
        }));
        assertEquals(2, mappedListeners.size());
        assertEquals(1, mappedListeners.stream()
                .map(MappedJobListener::getListener)
                .filter(l -> l instanceof JobLogListener)
                .count());
    }

    public static class SharedState {
        private static StringBuilder BUFFER;

        static void reset() {
            BUFFER = new StringBuilder();
        }

        static void append(String value) {
            BUFFER.append(value);
        }

        static String getAndReset() {
            String val = BUFFER.toString();
            reset();
            return val;
        }
    }

    public static class Listener1 implements JobListener {
        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
            finishEventSource.accept(result -> SharedState.append("_L1_finished"));

            SharedState.append("_L1_started");
        }
    }

    public static class Listener2 implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
            finishEventSource.accept(result -> SharedState.append("_L2_finished"));
            SharedState.append("_L2_started");
        }
    }

    public static class Listener3 implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
            finishEventSource.accept(result -> SharedState.append("_L3_finished"));
            SharedState.append("_L3_started");
        }
    }
}
