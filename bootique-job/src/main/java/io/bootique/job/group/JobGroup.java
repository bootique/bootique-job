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

package io.bootique.job.group;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobResult;

import java.util.List;
import java.util.Map;

/**
 * @since 3.0
 */
public class JobGroup extends BaseJob {

    // linear steps, each one may be a single job or a set of parallel jobs
    private final List<JobGroupStep> steps;

    public JobGroup(JobMetadata groupMetadata, List<JobGroupStep> steps) {
        super(groupMetadata);
        this.steps = steps;
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        for (JobGroupStep step : steps) {
            JobGroupStepResult result = step.run(params);

            if (result.getOutcome() != JobGroupStepOutcome.succeeded) {
                return result.getJobResult();
            }
        }

        return JobResult.success(getMetadata());
    }
}
