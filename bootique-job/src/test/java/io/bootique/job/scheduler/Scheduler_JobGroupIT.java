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

package io.bootique.job.scheduler;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.job.fixture.ParameterizedJob3;
import io.bootique.job.fixture.ScheduledJob1;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runtime.JobModule;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class Scheduler_JobGroupIT {
    
    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app("-c", "classpath:io/bootique/job/config_jobgroup.yml")
            .module(JobModule.class)
            .module(b -> JobModule.extend(b).addJob(ScheduledJob1.class))
            .module(b -> JobModule.extend(b).addJob(ParameterizedJob3.class))
            .createRuntime();

    @Test
    public void testRunOnce_JobGroup() {
        Scheduler scheduler = app.getInstance(Scheduler.class);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("param2", 2);

        JobFuture future = scheduler.runOnce("group1", parameters);
        JobResult result = future.get(5L, TimeUnit.SECONDS);
        assertEquals(JobOutcome.SUCCESS, result.getOutcome());
    }

    @Test
    public void testRunOnce_EmptyGroup() {
        Scheduler scheduler = app.getInstance(Scheduler.class);

        JobFuture future = scheduler.runOnce("group2", Collections.emptyMap());
        JobResult result = future.get(5L, TimeUnit.SECONDS);
        assertEquals(JobOutcome.SUCCESS, result.getOutcome());
    }
}
