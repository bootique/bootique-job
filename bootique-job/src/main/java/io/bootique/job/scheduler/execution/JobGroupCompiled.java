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

package io.bootique.job.scheduler.execution;

import io.bootique.job.BaseJob;
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;

import java.util.*;

/**
 * @since 3.0
 */
class JobGroupCompiled extends BaseJob {

    private final JobGroupRunner runner;
    private final List<Set<JobExecution>> executionGroups;

    public static JobGroupCompiled create(
            String jobName,
            Scheduler scheduler,
            DIGraph<JobExecution> executionGraph,
            Collection<Job> standaloneJobs) {

        JobMetadata groupMetadata = groupMetadata(jobName, standaloneJobs);
        return new JobGroupCompiled(
                groupMetadata,
                new JobGroupRunner(scheduler, groupMetadata, jobsByName(standaloneJobs)),
                executionGraph.reverseTopSort());
    }

    JobGroupCompiled(JobMetadata metadata, JobGroupRunner runner, List<Set<JobExecution>> executionGroups) {
        super(metadata);
        this.runner = runner;
        this.executionGroups = executionGroups;
    }

    private static JobMetadata groupMetadata(String jobName, Collection<Job> jobs) {
        JobMetadata.Builder builder = JobMetadata.builder(jobName);
        for (Job job : jobs) {
            job.getMetadata().getParameters().forEach(builder::param);
        }
        return builder.build();
    }

    private static Map<String, Job> jobsByName(Collection<Job> jobs) {
        Map<String, Job> byName = new HashMap<>();
        jobs.forEach(j -> byName.put(j.getMetadata().getName(), j));
        return byName;
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        executionGroups.forEach(e -> runner.run(e, params));
        return JobResult.success(getMetadata());
    }
}
