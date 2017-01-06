package io.bootique.job;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.job.fixture.ExecutableJob;
import io.bootique.job.runtime.JobModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class BaseJobTest {

    protected void executeJobs(Collection<? extends Job> jobs, String... args) {

        BQRuntime runtime = Bootique.app(args).module(new JobModule()).module(binder -> {
            jobs.forEach(job -> JobModule.contributeJobs(binder).addBinding().toInstance(job));
        }).createRuntime();

        try {
            runtime.getRunner().run();
        } finally {
            runtime.shutdown();
        }
    }

    protected void assertExecutedInOrder(List<ExecutableJob> jobs) {
        if (jobs.isEmpty()) {
            return;
        }

        assertExecuted(jobs);

        List<ExecutableJob> jobList = new ArrayList<>(jobs);
        jobList.sort((j1, j2) -> {
            long diff = j1.getStartedAt() - j2.getStartedAt();
            assertNotEquals("Jobs started at the same time: " + collectNames(j1, j2), 0, diff);
            return (int)diff;
        });

        Iterator<ExecutableJob> iter = jobList.iterator();
        ExecutableJob previous = iter.next(), next;
        while (iter.hasNext()) {
            next = iter.next();
            assertTrue("Execution of jobs overlapped: " + collectNames(previous, next),
                    previous.getFinishedAt() < next.getFinishedAt());
        }

        try {
            assertArrayEquals(jobs.toArray(), jobList.toArray());
        } catch (Throwable e) {
            throw new RuntimeException("Expected: " + collectNames(jobs) + ", actual: " + collectNames(jobList));
        }
    }

    protected void assertExecuted(List<ExecutableJob> jobs) {
        jobs.forEach(job -> assertTrue("Job was not executed: " + job.getMetadata().getName(), job.isExecuted()));
    }

    protected void assertExecutedWithParams(ExecutableJob job, Map<String, Object> expectedParams) {
        assertExecuted(Collections.singletonList(job));
        assertEquals(expectedParams, job.getParams());
    }

    private static <T extends Job> String collectNames(T... jobs) {
        return collectNames(Arrays.asList(jobs));
    }

    private static String collectNames(List<? extends Job> jobs) {
        return jobs.stream().collect(
                StringBuilder::new,
                (acc, job) -> acc.append(acc.length() > 0 ? "," + job.getMetadata().getName() : job.getMetadata().getName()),
                (acc1, acc2) -> acc1.append(acc2.toString())).toString();
    }
}
