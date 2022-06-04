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
import io.bootique.job.JobMetadata;
import io.bootique.job.MappedJobListener;
import io.bootique.job.graph.JobGraphNode;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.scheduler.execution.DefaultJobRegistry;
import io.bootique.job.scheduler.execution.ParallelJobBatchStep;

import javax.inject.Provider;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @since 3.0
 */
public class InstrumentedJobRegistry extends DefaultJobRegistry {

    private final JobMDCManager mdcManager;
    private final JobMetricsManager metricsManager;

    public InstrumentedJobRegistry(
            Collection<Job> standaloneJobs,
            Map<String, JobGraphNode> jobDefinitions,
            Provider<Scheduler> scheduler,
            Collection<MappedJobListener> listeners,
            JobMDCManager mdcManager,
            JobMetricsManager metricsManager) {
        super(standaloneJobs, jobDefinitions, scheduler, listeners);

        this.mdcManager = mdcManager;
        this.metricsManager = metricsManager;
    }

    @Override
    protected Job decorateWithLogger(Job job) {
        // replace super logger with instrumented logger that records job timing and provides the MDC context
        return new JobLogger(job, mdcManager, metricsManager);
    }

    @Override
    protected ParallelJobBatchStep createParallelGroupStep(List<Job> stepJobs, JobMetadata groupMetadata) {
        return new TxIdAwareJobGroupStep(scheduler.get(), stepJobs, groupMetadata, mdcManager.getTransactionIdMDC());
    }
}
