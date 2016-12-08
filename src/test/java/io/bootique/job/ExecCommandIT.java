package io.bootique.job;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.job.job.ExecutableJob;
import io.bootique.job.job.Job1;
import io.bootique.job.job.Job2;
import io.bootique.job.job.Job3;
import io.bootique.job.runtime.JobModule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ExecCommandIT {

    @Test
    public void testExec_SingleJob() {
        Job1 job1 = new Job1(0);
        executeJobs(Collections.singleton(job1), "--exec", "--job=job1");
        assertTrue(job1.isExecuted());
    }

    @Test(expected = Throwable.class)
    public void testExec_MultipleJobs_Parallel() {
        testExec_MultipleJobs(false);
    }

    @Test
    public void testExec_MultipleJobs_Serial() {
        testExec_MultipleJobs(true);
    }

    /**
     * 1. jobs are submitted to the executor in the same order as they appear in program arguments
     * 2. jobs are more likely to be executed in the same order in which they are submitted to the executor
     * => to increase the prob. of other orders of execution we vary the execution time of each job
     **/
    private void testExec_MultipleJobs(boolean serial) {
        String[] args;
        if (serial) {
            args = new String[] {"--exec", "--job=job1", "--job=job2", "--job=job3", "--serial"};
        } else {
            args = new String[] {"--exec", "--job=job1", "--job=job2", "--job=job3"};
        }

        Random random = new Random(System.currentTimeMillis());
        PrimitiveIterator.OfInt runningTimes = IntStream.generate(() -> random.nextInt(1000) * 1000).iterator();

        Job1 job1;
        Job2 job2;
        Job3 job3;
        for (int i = 0; i < 100; i++) {
            job1 = new Job1(runningTimes.next());
            job2 = new Job2(runningTimes.next());
            job3 = new Job3(runningTimes.next());
            executeJobs(Arrays.asList(job1, job2, job3), args);
            assertExecutedInOrder(Arrays.asList(job1, job2, job3));
        }
    }

    private static void executeJobs(Collection<Job> jobs, String... args) {

        BQRuntime runtime = Bootique.app(args).module(new JobModule()).module(binder -> {
            jobs.forEach(job -> JobModule.contributeJobs(binder).addBinding().toInstance(job));
        }).createRuntime();

        try {
            runtime.getRunner().run();
        } finally {
            runtime.shutdown();
        }
    }

    private static void assertExecutedInOrder(List<ExecutableJob> jobs) {
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

    private static void assertExecuted(List<ExecutableJob> jobs) {
        jobs.forEach(job -> assertTrue(job.isExecuted()));
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
