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

class ExecutableJobGroup extends BaseJob {

    private final JobGroupRunner runner;
    private final List<Set<JobExecution>> executions;

    public static ExecutableJobGroup create(
            String jobName,
            Scheduler scheduler,
            DependencyGraph graph,
            Collection<Job> jobs) {

        JobMetadata groupMetadata = groupMetadata(jobName, jobs);
        return new ExecutableJobGroup(
                groupMetadata,
                new JobGroupRunner(scheduler, groupMetadata, jobsByName(jobs)),
                executions(graph));
    }

    ExecutableJobGroup(JobMetadata metadata, JobGroupRunner runner, List<Set<JobExecution>> executions) {
        super(metadata);
        this.runner = runner;
        this.executions = executions;
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

    private static List<Set<JobExecution>> executions(DependencyGraph graph) {
        List<Set<JobExecution>> executions = graph.topSort();
        Collections.reverse(executions);
        return executions;
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        executions.stream().forEach(e -> runner.execute(e, params));
        return JobResult.success(getMetadata());
    }
}
