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
import io.bootique.job.fixture.ExceptionJob;
import io.bootique.job.fixture.Job1;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.runtime.JobModuleExtender;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class MappedListenerIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @BeforeEach
    public void before() {
        SharedState.reset();
    }

    @Test
    public void testJobException() {
        ExceptionJob job = new ExceptionJob();
        Listener_JobResultCapture listener = new Listener_JobResultCapture();

        BQRuntime runtime = testFactory
                .app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job).addListener(listener))
                .createRuntime();

        JobResult result = runtime.getInstance(Scheduler.class).runOnce("exception").get();
        assertSame(result, listener.result);
        assertEquals(JobOutcome.FAILURE, result.getOutcome());
        assertTrue(result.getThrowable() instanceof RuntimeException);
        assertEquals(ExceptionJob.EXCEPTION_MESSAGE, result.getThrowable().getMessage());
    }

    @Test
    public void testAddListener_AlterParams() {
        String val = "12345_abcde";
        Job_ParamsChange job = new Job_ParamsChange();
        Listener_ParamsChange listener = new Listener_ParamsChange(val);

        testFactory.app("--exec", "--job=job_paramschange")
                .autoLoadModules()
                .module(b -> JobModule.extend(b)
                        .addJob(job)
                        .addListener(listener))
                .run();

        assertEquals(val, job.getActualParam());
    }

    @Test
    public void testAddMappedListener_Ordering1() {
        Job1 job1 = new Job1(0);
        Set<? extends Job> jobs = Collections.singleton(job1);

        testFactory.app("--exec", "--job=job1")
                .module(new JobModule())
                .module(binder -> {
                    JobModuleExtender extender = JobModule.extend(binder);
                    jobs.forEach(extender::addJob);

                    extender.addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                            .addMappedListener(new MappedJobListener<>(new Listener2(), 2))
                            .addMappedListener(new MappedJobListener<>(new Listener3(), 3));
                }).createRuntime()
                .run();

        assertTrue(job1.isExecuted());
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void testAddMappedListener_Ordering2() throws Exception {
        Job1 job1 = new Job1(0);
        Set<? extends Job> jobs = Collections.singleton(job1);

        testFactory.app("--exec", "--job=job1")
                .module(new JobModule())
                .module(binder -> {
                    JobModuleExtender extender = JobModule.extend(binder);
                    jobs.forEach(extender::addJob);

                    extender.addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                            .addMappedListener(new MappedJobListener<>(new Listener3(), 3))
                            .addMappedListener(new MappedJobListener<>(new Listener2(), 2));
                }).createRuntime()
                .run();

        assertTrue(job1.isExecuted());
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void testAddMappedListener_OrderingVsUnmapped() {
        Job1 job1 = new Job1(0);
        Set<? extends Job> jobs = Collections.singleton(job1);

        testFactory.app("--exec", "--job=job1")
                .module(new JobModule())
                .module(binder -> {
                    JobModuleExtender extender = JobModule.extend(binder);
                    jobs.forEach(extender::addJob);

                    extender.addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                            .addListener(new Listener2())
                            .addMappedListener(new MappedJobListener<>(new Listener3(), 2));
                }).createRuntime()
                .run();

        assertTrue(job1.isExecuted());
        assertEquals("_L1_started_L3_started_L2_started_L2_finished_L3_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void testEmbeddedLogListener_Ordering() {
        Job1 job1 = new Job1(0);
        Set<? extends Job> jobs = Collections.singleton(job1);

        BQRuntime runtime = testFactory.app("--exec", "--job=job1")
                .module(new JobModule())
                .module(binder -> {
                    JobModuleExtender extender = JobModule.extend(binder);
                    jobs.forEach(extender::addJob);

                    extender.addListener(new Listener3())
                            .addMappedListener(new MappedJobListener<>(new Listener2(), 1));
                }).createRuntime();

        runtime.run();

        assertTrue(job1.isExecuted());
        assertEquals("_L2_started_L3_started_L3_finished_L2_finished", SharedState.getAndReset());

        Set<MappedJobListener> listeners = runtime.getInstance(Key.get(new TypeLiteral<Set<MappedJobListener>>() {
        }));
        assertTrue(listeners.size() == 2);
        assertEquals(1, listeners.stream()
                .filter(l -> l.getListener() instanceof JobLogListener)
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
            finishEventSource.accept(result -> {
                SharedState.append("_L1_finished");
            });

            SharedState.append("_L1_started");
        }
    }

    public static class Listener2 implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
            finishEventSource.accept(result -> {
                SharedState.append("_L2_finished");
            });
            SharedState.append("_L2_started");
        }
    }

    public static class Listener3 implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
            finishEventSource.accept(result -> {
                SharedState.append("_L3_finished");
            });
            SharedState.append("_L3_started");
        }
    }

    public static class Job_ParamsChange implements Job {

        private String actualParam;

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(Job_ParamsChange.class);
        }

        public String getActualParam() {
            return actualParam;
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            this.actualParam = (String) params.get("LP");
            return JobResult.success(getMetadata());
        }
    }

    public static class Listener_ParamsChange implements JobListener {
        private final String setParam;

        public Listener_ParamsChange(String setParam) {
            this.setParam = setParam;
        }

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
            parameters.put("LP", setParam);
        }
    }

    public static class Listener_JobResultCapture implements JobListener {

        private JobResult result;

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry) {
            onFinishedCallbackRegistry.accept(r -> {
                this.result = r;
            });
        }
    }
}
