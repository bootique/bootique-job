package io.bootique.job;

import io.bootique.job.fixture.Job1;
import io.bootique.job.fixture.Job2;
import io.bootique.job.fixture.Job3;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ExecCommandIT extends BaseJobTest {

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
     * => to increase the prob. of other orders of execution (incl. overlapping executions)
     *    we make the first submitted job the most time-consuming
     **/
    private void testExec_MultipleJobs(boolean serial) {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("--exec", "--job=job1", "--job=job2", "--job=job3"));

        if (serial) {
            args.add("--serial");
        }

        Job1 job1;
        Job2 job2;
        Job3 job3;
        for (int i = 0; i < 100; i++) {
            job1 = new Job1(100000);
            job2 = new Job2(10000);
            job3 = new Job3(1000);
            executeJobs(Arrays.asList(job1, job2, job3), args.toArray(new String[args.size()]));
            assertExecutedInOrder(Arrays.asList(job1, job2, job3));
        }
    }

    @Test(expected = Exception.class)
    public void testExec_MultipleGroups_Parallel() {
        testExec_MultipleGroups(false);
    }

    @Test
    public void testExec_MultipleGroups_Serial() {
        testExec_MultipleGroups(true);
    }

    private void testExec_MultipleGroups(boolean serial) {
        List<String> args = new ArrayList<>();
        args.add("--config=classpath:io/bootique/job/config_exec.yml");
        args.addAll(Arrays.asList("--exec", "--job=group2", "--job=group1"));

        if (serial) {
            args.add("--serial");
        }

        Job1 job1;
        Job2 job2;
        Job3 job3;
        for (int i = 0; i < 100; i++) {
            job1 = new Job1();
            job2 = new Job2(1000);
            job3 = new Job3(10000);
            executeJobs(Arrays.asList(job1, job2, job3), args.toArray(new String[args.size()]));
            assertExecutedInOrder(Arrays.asList(job3, job2, job1));
        }
    }
}
