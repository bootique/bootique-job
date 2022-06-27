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

package io.bootique.job.scheduler;

import java.util.Map;
import java.util.Objects;

/**
 * Defines execution schedule for a given job.
 *
 * @since 3.0
 */
public abstract class Trigger {

    private final String jobName;
    private final String triggerName;
    private final Map<String, Object> params;

    public Trigger(
            String jobName,
            String triggerName,
            Map<String, Object> params) {

        this.jobName = Objects.requireNonNull(jobName);
        this.triggerName = Objects.requireNonNull(triggerName);
        this.params = Objects.requireNonNull(params);
    }

    public abstract <T> T accept(TriggerVisitor<T> visitor);

    public String getJobName() {
        return jobName;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
