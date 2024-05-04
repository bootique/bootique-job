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

import io.bootique.BQCoreModule;
import io.bootique.job.fixture.BaseTestJob;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Deprecated
@BQTest
public class ListenerIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @BeforeEach
    public void before() {
        SharedState.reset();
    }

    @Test
    public void addListener_AlterParams() {
        String val = "12345_abcde";
        Job_ParamsChange job = new Job_ParamsChange();
        Listener_ParamsChange listener = new Listener_ParamsChange(val);

        testFactory.app("--exec", "--job=job_paramschange")
                .autoLoadModules()
                .module(b -> JobsModule.extend(b)
                        .addJob(job)
                        .addListener(listener))
                .run();

        assertEquals(val, job.getActualParam());
    }

    @Test
    public void addMappedListener_Ordering1() {
        XJob job = new XJob();

        testFactory.app("--exec", "--job=x")
                .modules(new JobsModule(), new SchedulerModule())
                .module(binder -> JobsModule.extend(binder)
                        .addJob(job)
                        .addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                        .addMappedListener(new MappedJobListener<>(new Listener2(), 2))
                        .addMappedListener(new MappedJobListener<>(new Listener3(), 3)))
                .run();

        job.assertExecuted();
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void addMappedListener_Ordering2() {
        XJob job = new XJob();

        testFactory.app("--exec", "--job=x")
                .modules(new JobsModule(), new SchedulerModule())
                .module(binder -> JobsModule.extend(binder)
                        .addJob(job)
                        .addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                        .addMappedListener(new MappedJobListener<>(new Listener3(), 3))
                        .addMappedListener(new MappedJobListener<>(new Listener2(), 2)))
                .run();

        job.assertExecuted();
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void addMappedListener_AddListener_Ordering() {
        XJob job = new XJob();

        testFactory.app("--exec", "--job=x")
                .modules(new JobsModule(), new SchedulerModule())
                .module(binder -> JobsModule.extend(binder).addJob(job)
                        .addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                        .addListener(new Listener2())
                        .addMappedListener(new MappedJobListener<>(new Listener3(), 2)))
                .run();

        job.assertExecuted();
        assertEquals("_L1_started_L3_started_L2_started_L2_finished_L3_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void addMappedListener_JobGroup_Ordering() {
        XJob x = new XJob();
        YJob y = new YJob();

        testFactory.app("--exec", "--job=g1")
                .autoLoadModules()
                .module(b -> BQCoreModule.extend(b).setProperty("bq.jobs.g1.type", "group")
                        .setProperty("bq.jobs.g1.jobs.x.type", "job")
                        .setProperty("bq.jobs.g1.jobs.y.type", "job"))
                .module(b -> JobsModule.extend(b)
                        .addJob(x)
                        .addJob(y)
                        .addMappedListener(new MappedJobListener<>(new Listener1(), 1))
                        .addMappedListener(new MappedJobListener<>(new Listener2(), 2))
                        .addMappedListener(new MappedJobListener<>(new Listener3(), 3)))
                .run();

        x.assertExecuted();
        y.assertExecuted();

        // no listeners should be called for subjobs of a group
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
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
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry) {
            onFinishedCallbackRegistry.accept(result -> SharedState.append("_L1_finished"));

            SharedState.append("_L1_started");
        }
    }

    public static class Listener2 implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry) {
            onFinishedCallbackRegistry.accept(result -> SharedState.append("_L2_finished"));
            SharedState.append("_L2_started");
        }
    }

    public static class Listener3 implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry) {
            onFinishedCallbackRegistry.accept(result -> SharedState.append("_L3_finished"));
            SharedState.append("_L3_started");
        }
    }

    static class Job_ParamsChange implements Job {

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
            return JobResult.succeeded();
        }
    }

    static class Listener_ParamsChange implements JobListener {
        private final String setParam;

        public Listener_ParamsChange(String setParam) {
            this.setParam = setParam;
        }

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry) {
            parameters.put("LP", setParam);
        }
    }

    static class XJob extends BaseTestJob<XJob> {
        public XJob() {
            super(XJob.class);
        }
    }

    static class YJob extends BaseTestJob<YJob> {
        public YJob() {
            super(YJob.class);
        }
    }

    static class ZJob extends BaseTestJob<ZJob> {
        public ZJob() {
            super(ZJob.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return super.run(params);
        }
    }
}
