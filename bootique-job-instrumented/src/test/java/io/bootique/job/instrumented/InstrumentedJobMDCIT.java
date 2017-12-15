package io.bootique.job.instrumented;

import com.codahale.metrics.MetricRegistry;
import io.bootique.command.CommandOutcome;
import io.bootique.job.Job;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.runtime.JobModuleExtender;
import io.bootique.logback.LogbackModule;
import io.bootique.metrics.mdc.SafeTransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class InstrumentedJobMDCIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    private TransactionIdMDC transactionIdMDC;
    private TransactionIdGenerator idGenerator;

    @Before
    public void before() {
        this.transactionIdMDC = new TransactionIdMDC();

        int cpus = Runtime.getRuntime().availableProcessors();
        if (cpus < 1) {
            cpus = 1;
        } else if (cpus > 4) {
            cpus = 4;
        }

        this.idGenerator = new SafeTransactionIdGenerator(cpus);
    }

    protected CommandOutcome executeJobs(Collection<? extends Job> jobs, String... args) {
        InstrumentedJobListener listener = new InstrumentedJobListener(new MetricRegistry(), transactionIdMDC, idGenerator);

        return testFactory.app(args)
                .module(new LogbackModule())
                .module(new JobModule())
                .module(binder -> {
                    JobModuleExtender extender = JobModule.extend(binder).addListener(listener);
                    jobs.forEach(extender::addJob);
                }).createRuntime()
                .run();
    }

    protected void assertExecuted(List<Job1> jobs) {
        jobs.forEach(job -> assertTrue("Job was not executed: " + job.getMetadata().getName(), job.isExecuted()));
    }

    @Test
    public void testExecJob() {
        Job1 job1 = new Job1();
        String[] args = new String[]{
                "--exec", "--job=job1", "--config=classpath:io/bootique/job/instrumented/config.yml"};

        List<Job1> jobs = Collections.singletonList(job1);
        executeJobs(jobs, args);
        assertExecuted(jobs);
    }
}
