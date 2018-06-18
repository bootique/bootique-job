/**
 *  Licensed to ObjectStyle LLC under one
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

package io.bootique.job.fixture;

import io.bootique.job.JobListener;
import io.bootique.job.runnable.JobResult;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class ExecutionRateListener implements JobListener {

    private final Deque<Execution> executions;

    private volatile double averageRate;

    public ExecutionRateListener() {
        this.executions = new LinkedBlockingDeque<>();
    }

    @Override
    public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
        Execution previousExecution = executions.peekLast();
        long startedAt = System.currentTimeMillis();

        finishEventSource.accept(result -> {
            executions.add(new Execution(startedAt, System.currentTimeMillis()));
            if (previousExecution != null) {
                recalculateAverageRate(startedAt - previousExecution.getFinishedAt());
            }
        });
    }

    private synchronized void recalculateAverageRate(long sample) {
        averageRate = rollingAverage(averageRate, sample, executions.size());
    }

    private double rollingAverage(double currentValue, double sample, int totalSamples) {
        currentValue -= currentValue / totalSamples;
        currentValue += sample / totalSamples;
        return currentValue;
    }

    public synchronized void reset() {
        this.executions.clear();
        this.averageRate = 0;
    }

    public long getAverageRate() {
        return (long) averageRate;
    }

    private static class Execution {
        private final long startedAt;
        private final long finishedAt;

        public Execution(long finishedAt, long startedAt) {
            this.finishedAt = finishedAt;
            this.startedAt = startedAt;
        }

        public long getStartedAt() {
            return startedAt;
        }

        public long getFinishedAt() {
            return finishedAt;
        }
    }
}
