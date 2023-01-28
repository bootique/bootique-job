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
package io.bootique.job.trigger;

/**
 * @since 3.0
 */
public class FixedRateTrigger extends Trigger {

    private final long fixedRateMs;
    private final long initialDelayMs;

    public FixedRateTrigger(
            JobExec exec,
            String triggerName,
            long fixedRateMs,
            long initialDelayMs) {

        super(exec, triggerName);
        this.fixedRateMs = fixedRateMs;
        this.initialDelayMs = initialDelayMs;
    }

    @Override
    public <T> T accept(TriggerVisitor<T> visitor) {
        return visitor.visitFixedRate(this);
    }

    public long getFixedRateMs() {
        return fixedRateMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    @Override
    public String toString() {
        return "fixed rate trigger " + fixedRateMs + " ms";
    }
}
