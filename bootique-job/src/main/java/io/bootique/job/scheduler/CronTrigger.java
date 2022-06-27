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

import io.bootique.job.value.Cron;

import java.util.Map;
import java.util.Objects;

/**
 * @since 3.0
 */
public class CronTrigger extends Trigger {

    private final Cron cron;

    public CronTrigger(
            String jobName,
            String triggerName,
            Map<String, Object> params,
            Cron cron) {

        super(jobName, triggerName, params);
        this.cron = Objects.requireNonNull(cron);
    }

    @Override
    protected org.springframework.scheduling.Trigger springTrigger() {
        return new org.springframework.scheduling.support.CronTrigger(cron.getExpression());
    }

    @Override
    public String toString() {
        return "cron trigger " + cron.getExpression();
    }
}
