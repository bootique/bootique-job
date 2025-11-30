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

import io.bootique.job.JobRegistry;
import io.bootique.job.TriggerBuilder;
import io.bootique.job.trigger.CronExpression;
import io.bootique.job.trigger.CronTrigger;
import io.bootique.job.trigger.Trigger;

import java.util.Objects;
import java.util.function.Consumer;

class CronTriggerBuilder extends TriggerBuilder {

    private final String cron;

    public CronTriggerBuilder(
            Consumer<Trigger> addToSchedulerCallback,
            JobRegistry jobRegistry,
            TaskScheduler taskScheduler,
            String cron) {
        super(addToSchedulerCallback, jobRegistry, taskScheduler);
        this.cron = Objects.requireNonNull(cron);
    }

    @Override
    protected CronTrigger makeTrigger() {
        return new CronTrigger(
                jobRegistry,
                taskScheduler,
                createJobName(),
                createTriggerName(),
                createParams(),
                CronExpression.parse(cron)
        );
    }
}
