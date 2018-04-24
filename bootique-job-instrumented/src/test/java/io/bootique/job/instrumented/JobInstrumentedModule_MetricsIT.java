package io.bootique.job.instrumented;

import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runtime.JobModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class JobInstrumentedModule_MetricsIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testMetrics() {
        BQRuntime runtime = testFactory
                .app("--exec", "--job", "J1")
                .autoLoadModules()
                .module(b -> JobModule.extend(b).addJob(J1.class))
                .createRuntime();

        runtime.run();

        MetricRegistry metricRegistry = runtime.getInstance(MetricRegistry.class);

        Set<String> expectedTimers = new HashSet<>(asList("bq.Job.J1.Time"));
        assertEquals(expectedTimers, metricRegistry.getTimers().keySet());

        Set<String> expectedCounters = new HashSet<>(asList(
                "bq.Job.J1.Success",
                "bq.Job.J1.Failure",
                "bq.Job.J1.Active",
                "bq.Job.J1.Completed"));

        assertEquals(expectedCounters, metricRegistry.getCounters().keySet());
    }

    public static final class J1 implements Job {

        private JobMetadata metadata = JobMetadata.builder("J1").build();

        @Override
        public JobMetadata getMetadata() {
            return metadata;
        }

        @Override
        public JobResult run(Map<String, Object> parameters) {
            return JobResult.success(metadata);
        }
    }
}
