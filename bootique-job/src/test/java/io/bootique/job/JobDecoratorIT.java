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

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class JobDecoratorIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @BeforeEach
    public void before() {
        SharedState.reset();
    }

    @Test
    public void addDecorator_AlterParams() {
        String val = "12345_abcde";
        Job_ParamsChange job = new Job_ParamsChange();
        Listener_ParamsChange listener = new Listener_ParamsChange(val);

        testFactory.app("--exec", "--job=job_paramschange")
                .autoLoadModules()
                .module(b -> JobsModule.extend(b).addJob(job).addDecorator(listener))
                .run();

        assertEquals(val, job.getActualParam());
    }

    @Test
    public void addMappedDecorator_Ordering1() {
        XJob job = new XJob();

        testFactory.app("--exec", "--job=x")
                .modules(new JobsModule(), new SchedulerModule())
                .module(binder -> JobsModule.extend(binder)
                        .addJob(job)
                        .addMappedDecorator(new MappedJobDecorator<>(new Listener1(), 1))
                        .addMappedDecorator(new MappedJobDecorator<>(new Listener2(), 2))
                        .addMappedDecorator(new MappedJobDecorator<>(new Listener3(), 3)))
                .run();

        job.assertExecuted();
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void addMappedDecorator_Ordering2() {
        XJob job = new XJob();

        testFactory.app("--exec", "--job=x")
                .modules(new JobsModule(), new SchedulerModule())
                .module(binder -> JobsModule.extend(binder)
                        .addJob(job)
                        .addMappedDecorator(new MappedJobDecorator<>(new Listener1(), 1))
                        .addMappedDecorator(new MappedJobDecorator<>(new Listener3(), 3))
                        .addMappedDecorator(new MappedJobDecorator<>(new Listener2(), 2)))
                .run();

        job.assertExecuted();
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void addMappedDecorator_AddListener_Ordering() {
        XJob job = new XJob();

        testFactory.app("--exec", "--job=x")
                .modules(new JobsModule(), new SchedulerModule())
                .module(binder -> JobsModule.extend(binder).addJob(job)
                        .addMappedDecorator(new MappedJobDecorator<>(new Listener1(), 1))
                        .addDecorator(new Listener2())
                        .addMappedDecorator(new MappedJobDecorator<>(new Listener3(), 2)))
                .run();

        job.assertExecuted();
        assertEquals("_L1_started_L3_started_L2_started_L2_finished_L3_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void addDecorator_JobGroup_Ordering() {
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
                        .addDecorator(new Listener1(), 1)
                        .addDecorator(new Listener2(), 2)
                        .addDecorator(new Listener3(), 3))
                .run();

        x.assertExecuted();
        y.assertExecuted();

        // no listeners should be called for subjobs of a group
        assertEquals("_L1_started_L2_started_L3_started_L3_finished_L2_finished_L1_finished", SharedState.getAndReset());
    }

    @Test
    public void addDecorator_SlowParallelJob() {
        XJob x = new XJob();
        YJob y = new YJob();
        ZJob z = new ZJob();

        testFactory.app("--exec", "--job=g1")
                .autoLoadModules()
                .module(b -> BQCoreModule.extend(b)
                        .setProperty("bq.jobs.g1.type", "group")
                        .setProperty("bq.jobs.g1.jobs.x.type", "job")
                        .setProperty("bq.jobs.g1.jobs.y.type", "job")
                        .setProperty("bq.jobs.g1.jobs.z.type", "job"))
                .module(b -> JobsModule.extend(b)
                        .addJob(x)
                        .addJob(y)
                        .addJob(z)
                        .addDecorator(new Listener1()))
                .run();

        x.assertExecuted();
        y.assertExecuted();
        z.assertExecuted();

        assertEquals("_L1_started_L1_finished", SharedState.getAndReset());
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

    public static class Listener1 implements JobDecorator {

        @Override
        public JobOutcome run(Job delegate, Map<String, Object> params) {
            SharedState.append("_L1_started");
            try {
                return delegate.run(params);
            } finally {
                SharedState.append("_L1_finished");
            }
        }
    }

    public static class Listener2 implements JobDecorator {

        @Override
        public JobOutcome run(Job delegate, Map<String, Object> params) {
            SharedState.append("_L2_started");
            try {
                return delegate.run(params);
            } finally {
                SharedState.append("_L2_finished");
            }
        }
    }

    public static class Listener3 implements JobDecorator {

        @Override
        public JobOutcome run(Job delegate, Map<String, Object> params) {
            SharedState.append("_L3_started");
            try {
                return delegate.run(params);
            } finally {
                SharedState.append("_L3_finished");
            }
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
        public JobOutcome run(Map<String, Object> params) {
            this.actualParam = (String) params.get("LP");
            return JobOutcome.succeeded();
        }
    }

    static class Listener_ParamsChange implements JobDecorator {
        private final String setParam;

        public Listener_ParamsChange(String setParam) {
            this.setParam = setParam;
        }

        @Override
        public JobOutcome run(Job delegate, Map<String, Object> params) {
            params.put("LP", setParam);
            return delegate.run(params);
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
        public JobOutcome run(Map<String, Object> params) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return super.run(params);
        }
    }
}
