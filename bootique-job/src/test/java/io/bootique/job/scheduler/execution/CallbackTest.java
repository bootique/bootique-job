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

package io.bootique.job.scheduler.execution;

import io.bootique.job.*;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJob;
import io.bootique.job.runnable.RunnableJobFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CallbackTest {

    private RunnableJobFactory rjf;
    private MappedJobListener jobStats;

    private ExecutorService executor;

    @BeforeEach
    public void before() {
        this.jobStats = new MappedJobListener<>(new JobStats(), Integer.MAX_VALUE);
        this.rjf = (job, parameters) -> () -> Callback.runAndNotify(job, parameters, Collections.singleton(jobStats));
        this.executor = Executors.newFixedThreadPool(50);
    }

    @AfterEach
    public void after() {
        this.executor.shutdownNow();
    }

    @Test
    public void testListeners() throws InterruptedException {
        int jobCount = 1000;
        CountDownLatch latch = new CountDownLatch(jobCount);

        List<Runnable> jobs = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            Job job = (i % 2 == 0) ? TestJob.withOutcome("goodjob", JobOutcome.SUCCESS) : TestJob.failing("badjob");
            RunnableJob rj = rjf.runnable(job, Collections.emptyMap());
            jobs.add(() -> {
                try {
                    rj.run();
                } catch (Throwable e) {
                    // ignore
                } finally {
                    latch.countDown();
                }
            });
        }
        Collections.shuffle(jobs);

        jobs.forEach(executor::execute);
        latch.await();

        JobStats listener = (JobStats) jobStats.getListener();
        listener.assertHasResults("goodjob", Collections.singletonMap(JobOutcome.SUCCESS, 500));
        listener.assertHasResults("badjob", Collections.singletonMap(JobOutcome.FAILURE, 500));
    }

    private static class TestJob extends BaseJob {

        public static TestJob withOutcome(String name, JobOutcome outcome) {
            return new TestJob(name, outcome);
        }

        public static TestJob failing(String name) {
            return new TestJob(name);
        }

        private JobOutcome outcome;
        private boolean shouldFail;

        private TestJob(String name, JobOutcome outcome) {
            super(JobMetadata.build(name));
            this.outcome = outcome;
        }

        private TestJob(String name) {
            super(JobMetadata.build(name));
            this.shouldFail = true;
        }

        @Override
        public JobResult run(Map<String, Object> parameters) {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(50));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (shouldFail) {
                throw new RuntimeException();
            } else {
                return new JobResult(getMetadata(), outcome, null, null);
            }
        }
    }

    private static class JobStats implements JobListener {

        private Map<String, Set<JobResult>> allResults;

        public JobStats() {
            this.allResults = new ConcurrentHashMap<>();
        }

        @Override
        public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> callback) {
            callback.accept(result -> getResults(jobName).add(result));
        }

        public Set<JobResult> getResults(String jobName) {
            Set<JobResult> results = allResults.get(jobName);
            if (results == null) {
                results = ConcurrentHashMap.newKeySet();
                Set<JobResult> existing = allResults.putIfAbsent(jobName, results);
                if (existing != null) {
                    results = existing;
                }
            }
            return results;
        }

        public void assertHasResults(String jobName, Map<JobOutcome, Integer> outcomes) {
            Set<JobResult> results = allResults.get(jobName);
            if (results == null) {
                throw new IllegalArgumentException("Unknown job: " + jobName);
            }

            Map<JobOutcome, Integer> realOutcomes = results.stream()
                    .collect(HashMap::new, (m, r) -> {
                        Integer count = m.get(r.getOutcome());
                        m.put(r.getOutcome(), (count == null ? 1 : ++count));
                    }, Map::putAll);

            assertEquals(realOutcomes, outcomes);
        }
    }
}
