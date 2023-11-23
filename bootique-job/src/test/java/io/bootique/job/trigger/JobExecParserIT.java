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
package io.bootique.job.trigger;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobModule;
import io.bootique.job.JobResult;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BQTest
public class JobExecParserIT {

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(b -> JobModule.extend(b).addJob(J1.class).addJob(J2.class))
            .createRuntime();

    final JobExecParser parser = app.getInstance(JobExecParser.class);

    @Test
    public void parseNull() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
    }

    @Test
    public void parseJobNameOnly() {
        JobExec exec = parser.parse("j1");
        assertEquals("j1", exec.getJobName());
        assertEquals(Map.of(), exec.getParams());
    }

    @Test
    public void parseJobNameOnly_TrailingColon() {
        JobExec exec = parser.parse("j1{");
        assertEquals("j1", exec.getJobName());
        assertEquals(Map.of(), exec.getParams());
    }

    @Test
    public void parseJobWithParams() {
        JobExec exec = parser.parse("j1{\"p1\":\"a1\",\"p2\":3}");
        assertEquals("j1", exec.getJobName());
        assertEquals(Map.of("p1", "a1", "p2", 3), exec.getParams());
    }

    @Test
    public void parseJobWithParams_Conversion() {
        JobExec exec = parser.parse("j2{\"date\":\"2023-02-15\",\"time\":\"16:00:01\",\"int\":4}");
        assertEquals("j2", exec.getJobName());
        assertEquals(Map.of(
                "date", LocalDate.of(2023, 2, 15),
                "time", LocalTime.of(16, 0, 1),
                "int", Integer.valueOf(4)), exec.getParams());
    }

    static class J1 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.build(J1.class);
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            return JobResult.success(getMetadata());
        }
    }

    static class J2 implements Job {

        @Override
        public JobMetadata getMetadata() {
            return JobMetadata.builder(J2.class)
                    .param("int", "int", Integer::parseInt)
                    .param("date", "date", LocalDate::parse)
                    .param("time", "time", LocalTime::parse)
                    .build();
        }

        @Override
        public JobResult run(Map<String, Object> params) {
            return JobResult.success(getMetadata());
        }
    }
}
