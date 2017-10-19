package io.bootique.job;

import io.bootique.command.CommandOutcome;
import io.bootique.job.fixture.ExecutableAtMostOnceJob;
import io.bootique.job.fixture.Job1;
import io.bootique.job.fixture.Job2;
import io.bootique.job.fixture.Job3;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class ExecCommandIT extends BaseJobExecIT {

    @Test
    public void testExec_SingleJob() {
        Job1 job1 = new Job1(0);
        executeJobs(Collections.singleton(job1), "--exec", "--job=job1");
        assertTrue(job1.isExecuted());
    }

    @Test
    public void testExec_SingleJob_Failure() {
        Job1 job1 = new Job1(0, true);
        CommandOutcome outcome = executeJobs(Collections.singleton(job1), "--exec", "--job=job1");
        assertTrue(job1.isExecuted());
        assertFailed(outcome);
    }

    @Test
    public void testExec_MultipleJobs_Parallel() {
        testExec_MultipleJobs(false, false);
    }

    @Test
    public void testExec_MultipleJobs_Parallel_Failure() {
        testExec_MultipleJobs(false, true);
    }

    @Test
    public void testExec_MultipleJobs_Serial() {
        testExec_MultipleJobs(true, false);
    }

    @Test
    public void testExec_MultipleJobs_Serial_Failure() {
        testExec_MultipleJobs(true, true);
    }

    /**
     * 1. jobs are submitted to the executor in the same order as they appear in program arguments
     * 2. jobs are more likely to be executed in the same order in which they are submitted to the executor
     * => to increase the prob. of other orders of execution (incl. overlapping executions)
     *    we make the first submitted job the most time-consuming
     **/
    private void testExec_MultipleJobs(boolean serial, boolean shouldFail) {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("--exec", "--job=job1", "--job=job2", "--job=job3"));

        if (serial) {
            args.add("--serial");
        }

        Job1 job1;
        Job2 job2;
        Job3 job3;
        int jobCount = 3;
        boolean firstShouldFail;
        boolean secondShouldFail;
        boolean thirdShouldFail;

        for (int i = 0; i < 100; i++) {
            firstShouldFail = shouldFail && randomizedBoolean(jobCount);
            secondShouldFail = shouldFail && randomizedBoolean(jobCount);
            thirdShouldFail = shouldFail && randomizedBoolean(jobCount);

            if (shouldFail && !firstShouldFail && !secondShouldFail && !thirdShouldFail) {
                switch (new Random().nextInt(3)) {
                    case 0:
                        firstShouldFail = true;
                        break;
                    case 1:
                        secondShouldFail = true;
                        break;
                    case 2:
                        thirdShouldFail = true;
                        break;
                }
            }

            job1 = new Job1(100000, firstShouldFail);
            job2 = new Job2(10000, secondShouldFail);
            job3 = new Job3(1000, thirdShouldFail);

            List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job1, job2, job3);
            CommandOutcome outcome = executeJobs(jobs, args.toArray(new String[args.size()]));

            if (serial) {
                List<ExecutableAtMostOnceJob> expectedToExecute = new ArrayList<>();
                List<ExecutableAtMostOnceJob> notExpectedToExecute = new ArrayList<>();

                boolean previousHasFailed = false;
                for (ExecutableAtMostOnceJob job : jobs) {
                    if (!previousHasFailed && job.shouldFail()) {
                        expectedToExecute.add(job);
                    } else if (previousHasFailed) {
                        notExpectedToExecute.add(job);
                    } else {
                        expectedToExecute.add(job);
                    }
                    previousHasFailed = previousHasFailed || job.shouldFail();
                }

                assertExecutedInOrder(expectedToExecute);
                assertNotExecuted(notExpectedToExecute);
            } else {
                assertExecuted(jobs);
            }

            if (shouldFail) {
                assertFailed(outcome);
            } else {
                assertSuccess(outcome);
            }
        }
    }

    private boolean randomizedBoolean(int inverseProb) {
        return new Random().nextInt(inverseProb) == 0;
    }

    @Test
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

            List<ExecutableAtMostOnceJob> jobs = Arrays.asList(job3, job2, job1);
            CommandOutcome outcome = executeJobs(jobs, args.toArray(new String[args.size()]));

            if (serial) {
                assertExecutedInOrder(jobs);
            } else {
                assertExecuted(jobs);
            }

            assertSuccess(outcome);
        }
    }
}
