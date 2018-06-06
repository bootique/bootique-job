/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.job.instrumented;

import io.bootique.job.JobListener;
import io.bootique.job.runnable.JobResult;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @since 0.25
 */
public class JobMDCManager implements JobListener {
    private TransactionIdGenerator idGenerator;
    private TransactionIdMDC transactionIdMDC;

    public JobMDCManager(TransactionIdGenerator idGenerator, TransactionIdMDC transactionIdMDC) {
        this.idGenerator = idGenerator;
        this.transactionIdMDC = transactionIdMDC;
    }

    @Override
    public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
        String id = idGenerator.nextId();
        transactionIdMDC.reset(id);
    }
}