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

package io.bootique.job.runnable;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

/**
 * @since 0.24
 */
public class JobFutureBuilder {

    private String job;
    private RunnableJob runnable;
    private ScheduledFuture<?> future;
    private Supplier<JobResult> resultSupplier;

    public JobFutureBuilder(String job) {
        this.job = Objects.requireNonNull(job);
    }

    public JobFutureBuilder runnable(RunnableJob runnable) {
        this.runnable = Objects.requireNonNull(runnable);
        return this;
    }

    public JobFutureBuilder future(ScheduledFuture<?> future) {
        this.future = future;
        return this;
    }

    public JobFutureBuilder resultSupplier(Supplier<JobResult> resultSupplier) {
        this.resultSupplier = resultSupplier;
        return this;
    }

    public JobFuture build() {
        Objects.requireNonNull(future);
        Objects.requireNonNull(resultSupplier);
        return new DefaultJobFuture(job, runnable, future, resultSupplier);
    }
}
