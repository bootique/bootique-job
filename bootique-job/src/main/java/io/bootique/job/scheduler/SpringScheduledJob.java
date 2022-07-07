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

import io.bootique.job.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.concurrent.ScheduledFuture;

/**
 * @since 3.0
 */
public class SpringScheduledJob extends BaseScheduledJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringScheduledJob.class);

    private final TaskScheduler taskScheduler;
    private final TriggerVisitor<org.springframework.scheduling.Trigger> springTriggerCompiler;

    public SpringScheduledJob(Job job, TaskScheduler taskScheduler) {

        super(job);

        this.taskScheduler = taskScheduler;
        this.springTriggerCompiler = new TriggerVisitor<>() {
            @Override
            public org.springframework.scheduling.Trigger visitCron(CronTrigger trigger) {
                return new org.springframework.scheduling.support.CronTrigger(trigger.getCron().getExpression());
            }

            @Override
            public org.springframework.scheduling.Trigger visitFixedRate(FixedRateTrigger trigger) {
                PeriodicTrigger pt = new PeriodicTrigger(trigger.getFixedRateMs());
                pt.setFixedRate(true);
                pt.setInitialDelay(trigger.getInitialDelayMs());
                return pt;
            }

            @Override
            public org.springframework.scheduling.Trigger visitFixedDelay(FixedDelayTrigger trigger) {
                PeriodicTrigger pt = new PeriodicTrigger(trigger.getFixedDelayMs());
                pt.setFixedRate(false);
                pt.setInitialDelay(trigger.getInitialDelayMs());
                return pt;
            }
        };
    }

    @Override
    protected ScheduledJobState doSchedule(Job job, Trigger trigger) {
        LOGGER.info(String.format("Will schedule '%s'.. (%s)", getJobName(), trigger));
        ScheduledFuture<?> future = taskScheduler.schedule(() -> job.run(trigger.getParams()), trigger.accept(springTriggerCompiler));
        return ScheduledJobState.scheduled(trigger, future);
    }
}
