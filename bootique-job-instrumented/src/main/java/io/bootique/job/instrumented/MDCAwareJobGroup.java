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
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.scheduler.execution.JobGroup;
import io.bootique.metrics.mdc.TransactionIdMDC;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since 3.0
 */
public class MDCAwareJobGroup extends JobGroup {

    private final TransactionIdMDC transactionIdMDC;

    public MDCAwareJobGroup(JobMetadata groupMetadata, List<Set<Job>> executionPlan, Scheduler scheduler, TransactionIdMDC transactionIdMDC) {
        super(groupMetadata, executionPlan, scheduler);
        this.transactionIdMDC = transactionIdMDC;
    }

    @Override
    protected JobFuture submitGroupMember(Job job, Map<String, Object> params) {

        // this is called within a "run" method, so capture the current MDC 
        // and pass it down to whatever thread runs it

        Job withInheritedTxId = decorateWithGroupTxId(job);

        return super.submitGroupMember(withInheritedTxId, params);
    }

    protected Job decorateWithGroupTxId(Job job) {
        return MDCAwareGroupMemberJobDecorator.captureCurrentTxId(job, transactionIdMDC);
    }
}
