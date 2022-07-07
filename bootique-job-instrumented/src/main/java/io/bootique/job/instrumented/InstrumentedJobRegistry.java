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

import io.bootique.job.Job;
import io.bootique.job.graph.JobGraphNode;
import io.bootique.job.runtime.JobDecorators;
import io.bootique.job.Scheduler;
import io.bootique.job.runtime.DefaultJobRegistry;
import io.bootique.job.group.ParallelJobBatchStep;

import javax.inject.Provider;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @since 3.0
 */
public class InstrumentedJobRegistry extends DefaultJobRegistry {

    private final JobMDCManager mdcManager;

    public InstrumentedJobRegistry(
            Collection<Job> standaloneJobs,
            Map<String, JobGraphNode> jobDefinitions,
            Provider<Scheduler> scheduler,
            JobDecorators decorators,
            JobMDCManager mdcManager) {
        super(standaloneJobs, jobDefinitions, scheduler, decorators);

        this.mdcManager = mdcManager;
    }

    @Override
    protected ParallelJobBatchStep createParallelGroupStep(List<Job> stepJobs) {
        return new TxIdAwareJobGroupStep(scheduler.get(), stepJobs, mdcManager.getTransactionIdMDC());
    }
}
