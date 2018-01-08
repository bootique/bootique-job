package io.bootique.job;

import io.bootique.job.fixture.Job1;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.runtime.JobModuleExtender;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MappedListenerIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Before
    public void before() {
        SharedState.reset();
    }

    @Test
    public void testAddMappedListener_Ordering1() throws Exception {
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
        assertEquals("_L1_started_L2_started_L3_started", SharedState.getAndReset());
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
        assertEquals("_L1_started_L2_started_L3_started", SharedState.getAndReset());
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
                            .addListener(new Listener3())
                            .addMappedListener(new MappedJobListener<>(new Listener2(), 2));
                }).createRuntime()
                .run();

        assertTrue(job1.isExecuted());
        assertEquals("_L1_started_L2_started_L3_started", SharedState.getAndReset());
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
        private Map<String, Set<JobResult>> allResults;

        public Listener1() {
            this.allResults = new ConcurrentHashMap<>();
        }

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
            finishEventSource.accept(result -> {
            });

            SharedState.append("_L1_started");
        }
    }

    public static class Listener2 implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
            SharedState.append("_L2_started");
        }
    }

    public static class Listener3 implements JobListener {

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
            SharedState.append("_L3_started");
        }
    }

}
