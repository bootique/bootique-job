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
import io.bootique.job.fixture.BaseTestJob;
import io.bootique.job.runtime.JobDecorators;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class JobDecorator_FailuresIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testJobFailure() {
        FailureJob job = new FailureJob();
        JobResultCapture listener = new JobResultCapture();

        BQRuntime runtime = testFactory
                .app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job).addDecorator(listener, JobDecorators.PARAMS_BINDER_ORDER + 1))
                .createRuntime();

        JobResult result = runtime.getInstance(Scheduler.class).runBuilder().jobName("failure").runNonBlocking().get();
        assertSame(result, listener.result);
        assertEquals(JobOutcome.FAILURE, result.getOutcome());
        assertNull(result.getThrowable());
        assertEquals(FailureJob.FAILURE_MESSAGE, result.getMessage());
    }

    @Test
    public void testListenerException_OnStart() {
        XJob job = new XJob();

        BQRuntime runtime = testFactory
                .app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job).addDecorator(new StartException(), JobDecorators.PARAMS_BINDER_ORDER + 1))
                .createRuntime();

        JobResult result = runtime.getInstance(Scheduler.class).runBuilder().jobName("x").runNonBlocking().get();
        job.assertNotExecuted();

        assertEquals(JobOutcome.FAILURE, result.getOutcome());
        assertEquals("This decorator always throws on start", result.getThrowable().getMessage());
    }

    @Test
    public void testListenerException_OnFinish() {
        XJob job = new XJob();

        BQRuntime runtime = testFactory
                .app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job).addDecorator(new EndException(), JobDecorators.PARAMS_BINDER_ORDER + 1))
                .createRuntime();

        JobResult result = runtime.getInstance(Scheduler.class).runBuilder().jobName("x").runNonBlocking().get();
        job.assertExecuted();

        assertEquals(JobOutcome.FAILURE, result.getOutcome());
        assertEquals("This decorator always throws on finish", result.getThrowable().getMessage());
    }

    public static class StartException implements JobDecorator {

        @Override
        public JobResult run(Job delegate, Map<String, Object> params) {
            throw new RuntimeException("This decorator always throws on start");
        }
    }

    public static class EndException implements JobDecorator {

        @Override
        public JobResult run(Job delegate, Map<String, Object> params) {
            try {
                return delegate.run(params);
            } finally {
                throw new RuntimeException("This decorator always throws on finish");
            }
        }
    }

    public static class JobResultCapture implements JobDecorator {

        private JobResult result;

        @Override
        public JobResult run(Job delegate, Map<String, Object> params) {

            this.result = delegate.run(params);
            return this.result;
        }
    }

    static class XJob extends BaseTestJob<XJob> {
        public XJob() {
            super(XJob.class);
        }
    }

    static class FailureJob extends BaseJob {

        public static final String FAILURE_MESSAGE = "Emulated Failure";

        public FailureJob() {
            super(JobMetadata.build(FailureJob.class));
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            return JobResult.failure(getMetadata(), FAILURE_MESSAGE);
        }
    }
}
