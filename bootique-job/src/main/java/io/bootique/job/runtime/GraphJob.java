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

package io.bootique.job.runtime;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobOutcome;

import java.util.List;
import java.util.Map;

/**
 * A job that is internally composed of a graph of multiple interdependent jobs.
 *
 * @since 3.0
 */
public class GraphJob extends BaseJob {

    // linear steps, each one may be a single job or a set of parallel jobs
    private final List<GraphJobStep> steps;

    public GraphJob(JobMetadata metadata, List<GraphJobStep> steps) {
        super(metadata);
        this.steps = steps;
    }

    @Override
    public JobOutcome run(Map<String, Object> params) {
        for (GraphJobStep step : steps) {
            JobOutcome result = step.run(params);

            if (!result.isSuccess()) {
                return result;
            }
        }

        return JobOutcome.succeeded();
    }
}
