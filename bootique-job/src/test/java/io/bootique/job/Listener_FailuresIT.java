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
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@Deprecated
@BQTest
public class Listener_FailuresIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void jobException() {
        ExceptionJob job = new ExceptionJob();
        Listener_JobResultCapture listener = new Listener_JobResultCapture();

        BQRuntime runtime = testFactory
                .app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job).addListener(listener))
                .createRuntime();

        JobResult result = runtime.getInstance(Scheduler.class).runBuilder().jobName("exception").runNonBlocking().get();
        assertSame(result, listener.result);
        assertEquals(JobOutcome.FAILURE, result.getOutcome());
        assertTrue(result.getThrowable() instanceof RuntimeException);
        assertEquals(ExceptionJob.EXCEPTION_MESSAGE, result.getThrowable().getMessage());
    }

    @Test
    public void jobFailure() {
        FailureJob job = new FailureJob();
        Listener_JobResultCapture listener = new Listener_JobResultCapture();

        BQRuntime runtime = testFactory
                .app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job).addListener(listener))
                .createRuntime();

        JobResult result = runtime.getInstance(Scheduler.class).runBuilder().jobName("failure").runNonBlocking().get();
        assertSame(result, listener.result);
        assertEquals(JobOutcome.FAILURE, result.getOutcome());
        assertNull(result.getThrowable());
        assertEquals(FailureJob.FAILURE_MESSAGE, result.getMessage());
    }

    @Test
    public void listenerException_OnStart() {
        XJob job = new XJob();

        BQRuntime runtime = testFactory
                .app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job).addListener(Listener_StartException.class))
                .createRuntime();

        JobResult result = runtime.getInstance(Scheduler.class).runBuilder().jobName("x").runNonBlocking().get();
        job.assertNotExecuted();

        assertEquals(JobOutcome.FAILURE, result.getOutcome());
        assertEquals("This listener always throws on start", result.getThrowable().getMessage());
    }

    @Test
    public void listenerException_OnFinish() {
        XJob job = new XJob();

        BQRuntime runtime = testFactory
                .app()
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(job).addListener(Listener_EndException.class))
                .createRuntime();

        JobResult result = runtime.getInstance(Scheduler.class).runBuilder().jobName("x").runNonBlocking().get();
        job.assertExecuted();

        assertEquals(JobOutcome.FAILURE, result.getOutcome());
        assertEquals("This listener always throws on finish", result.getThrowable().getMessage());
    }

    public static class Listener_StartException implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry) {
            throw new RuntimeException("This listener always throws on start");
        }
    }

    public static class Listener_EndException implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> onFinishedCallbackRegistry) {
            onFinishedCallbackRegistry.accept(r -> {
                throw new RuntimeException("This listener always throws on finish");
            });
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

    static class ExceptionJob extends BaseJob {

        public static final String EXCEPTION_MESSAGE = "Emulated Exception";

        public ExceptionJob() {
            super(JobMetadata.build(ExceptionJob.class));
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }
    }
}
