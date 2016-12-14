package io.bootique.job;

import io.bootique.job.fixture.Job1;
import io.bootique.job.fixture.Job2;
import io.bootique.job.fixture.Job3;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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
        String[] args;
        if (serial) {
            args = new String[] {"--exec", "--job=job1", "--job=job2", "--job=job3", "--serial"};
        } else {
            args = new String[] {"--exec", "--job=job1", "--job=job2", "--job=job3"};
        }

        Job1 job1;
        Job2 job2;
        Job3 job3;
        for (int i = 0; i < 100; i++) {
            job1 = new Job1(100000);
            job2 = new Job2(10000);
            job3 = new Job3(1000);
            executeJobs(Arrays.asList(job1, job2, job3), args);
            assertExecutedInOrder(Arrays.asList(job1, job2, job3));
        }
    }
}
