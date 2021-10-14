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

import com.codahale.metrics.Timer;
import io.bootique.job.runnable.JobResult;

import java.util.Objects;

/**
 * @since 3.0
 */
class JobMeter {

    private final JobMetrics metrics;
    private Timer.Context runTimer;

    public JobMeter(JobMetrics metrics) {
        this.metrics = Objects.requireNonNull(metrics);
    }

    public void start() {
        this.metrics.getActiveCounter().inc();
        this.runTimer = metrics.getTimer().time();
    }

    public long stop(JobResult result) {
        metrics.getActiveCounter().dec();
        metrics.getCompletedCounter().inc();

        // Timer.Context#stop also updates aggregate running time of all instances of <jobName>
        long timeNanos = runTimer.stop();

        switch (result.getOutcome()) {
            case SUCCESS: {
                metrics.getSuccessCounter().inc();
                break;
            }
            case FAILURE: {
                metrics.getFailureCounter().inc();
                break;
            }
            // do not track other results
        }

        // return in milliseconds
        return timeNanos / 1_000_000L;
    }
}
