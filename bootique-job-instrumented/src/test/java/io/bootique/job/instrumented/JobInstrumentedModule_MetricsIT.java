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

package io.bootique.job.instrumented;

import com.codahale.metrics.MetricRegistry;
import io.bootique.BQRuntime;
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobOutcome;
import io.bootique.job.JobsModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class JobInstrumentedModule_MetricsIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void metrics() {
        BQRuntime runtime = testFactory
                .app("--exec", "--job", "J1")
                .autoLoadModules()
                .module(b -> JobsModule.extend(b).addJob(J1.class))
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
        public JobOutcome run(Map<String, Object> parameters) {
            return JobOutcome.succeeded();
        }
    }
}
