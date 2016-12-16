package io.bootique.job;

import io.bootique.job.fixture.ExecutableJob;
import io.bootique.job.fixture.Job1;
import io.bootique.job.fixture.Job2;
import io.bootique.job.fixture.Job3;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ExecutionIT extends BaseJobTest {

    @Test
    public void testExecution_SingleJob_DefaultParams() {
        Job1 job1 = new Job1();
        String[] args = new String[] {"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=job1"};

        List<ExecutableJob> jobs = Collections.singletonList(job1);
        executeJobs(jobs, args);
        assertExecuted(jobs);
    }

    @Test
    public void testExecution_Group1_SingleJob_DefaultParams() {
        Job1 job1 = new Job1();
        String[] args = new String[] {"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group1"};

        List<ExecutableJob> jobs = Collections.singletonList(job1);
        executeJobs(jobs, args);
        assertExecuted(jobs);
    }

    @Test
    public void testExecution_Group2_MultipleJobs_Parallel_DefaultParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2();
        String[] args = new String[] {"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group2"};

        List<ExecutableJob> jobs = Arrays.asList(job1, job2);
        executeJobs(jobs, args);
        assertExecuted(jobs);
    }

    @Test
    public void testExecution_Group3_MultipleJobs_Parallel_OverridenParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2();
        String[] args = new String[] {"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group3"};

        List<ExecutableJob> jobs = Arrays.asList(job1, job2);
        executeJobs(jobs, args);
        assertExecutedWithParams(job1, new HashMap<String, Object>() {{
            put("a", "overriden");
            put("b", "default");
        }});
        assertExecutedWithParams(job2, new HashMap<String, Object>() {{
            put("e", "default");
            put("y", "added");
        }});
    }

    @Test
    public void testExecution_Group4_MultipleJobs_Dependent_OverridenParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        String[] args = new String[] {"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group4"};

        List<ExecutableJob> jobs = Arrays.asList(job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
        assertExecutedWithParams(job2, Collections.singletonMap("e", "overriden"));
    }

    @Test
    public void testExecution_Group5_MultipleJobs_Dependent_OverridenParams() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);
        String[] args = new String[] {"--config=classpath:io/bootique/job/config.yml", "--exec", "--job=group5"};

        List<ExecutableJob> jobs = Arrays.asList(job3, job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
        assertExecutedWithParams(job1, new HashMap<String, Object>() {{
            put("a", "default");
            put("b", "overriden");
        }});
        assertExecutedWithParams(job2, new HashMap<String, Object>() {{
            put("e", "default");
            put("z", "added");
        }});
        assertExecutedWithParams(job3, new HashMap<String, Object>() {{
            put("i", "default");
            put("k", "overriden");
            put("y", "added");
        }});
    }

    @Test
    public void testExecution_JobWithDependencies_1() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);
        String[] args = new String[] {"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=job1"};

        List<ExecutableJob> jobs = Arrays.asList(job3, job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_JobWithDependencies_2() {
        Job2 job2 = new Job2();
        Job3 job3 = new Job3(1000);
        String[] args = new String[] {"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=job2"};

        List<ExecutableJob> jobs = Arrays.asList(job3, job2);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_Group1_DefaultDependencies() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);
        String[] args = new String[] {"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=group1"};

        List<ExecutableJob> jobs = Arrays.asList(job3, job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_Group2_DefaultDependencies() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2(1000);
        Job3 job3 = new Job3(100000);
        String[] args = new String[] {"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=group2"};

        List<ExecutableJob> jobs = Arrays.asList(job3, job2, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_Group3_OverridenDependencies() {
        Job1 job1 = new Job1();
        Job3 job3 = new Job3(1000);
        String[] args = new String[] {"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=group3"};

        List<ExecutableJob> jobs = Arrays.asList(job3, job1);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }

    @Test
    public void testExecution_Group4_OverridenDependencies() {
        Job1 job1 = new Job1();
        Job2 job2 = new Job2();
        Job3 job3 = new Job3(1000);
        String[] args = new String[] {"--config=classpath:io/bootique/job/config_overriding_dependencies.yml", "--exec", "--job=group4"};

        List<ExecutableJob> jobs = Arrays.asList(job2, job1, job3);
        executeJobs(jobs, args);
        assertExecutedInOrder(jobs);
    }
}
