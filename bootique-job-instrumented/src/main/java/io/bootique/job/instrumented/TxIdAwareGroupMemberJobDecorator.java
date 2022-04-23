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
import io.bootique.job.runnable.JobResult;
import io.bootique.metrics.mdc.TransactionIdMDC;

import java.util.Map;

/**
 * @since 3.0
 */
class TxIdAwareGroupMemberJobDecorator implements Job {

    private final Job delegate;
    private final String groupMDC;
    private final TransactionIdMDC transactionIdMDC;

    public static TxIdAwareGroupMemberJobDecorator captureCurrentTxId(Job delegate, TransactionIdMDC transactionIdMDC) {
        return new TxIdAwareGroupMemberJobDecorator(delegate, transactionIdMDC.get(), transactionIdMDC);
    }

    TxIdAwareGroupMemberJobDecorator(Job delegate, String groupMDC, TransactionIdMDC transactionIdMDC) {
        this.delegate = delegate;
        this.groupMDC = groupMDC;
        this.transactionIdMDC = transactionIdMDC;
    }

    @Override
    public JobMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public JobResult run(Map<String, Object> parameters) {

        transactionIdMDC.reset(groupMDC);
        try {
            return delegate.run(parameters);
        } finally {
            transactionIdMDC.clear();
        }
    }
}