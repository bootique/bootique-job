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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.job.value.Cron;
import io.bootique.value.Duration;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * @since 3.0
 */
@BQConfig("Trigger of one of the following flavors: cron, periodic, fixed-rate.")
public class TriggerFactory {

    private String job;
    private String trigger;
    private Cron cron;
    private Duration fixedDelay;
    private Duration fixedRate;
    private Duration initialDelay;
    private Map<String, Object> params;

    public static String generateTriggerName() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public Trigger createTrigger() {

        String triggerName = this.trigger != null ? this.trigger : generateTriggerName();
        Map<String, Object> params = this.params != null ? this.params : Collections.emptyMap();
        long fixedDelayMs = fixedDelay != null && fixedDelay.getDuration() != null ? fixedDelay.getDuration().toMillis() : 0;
        long fixedRateMs = fixedRate != null && fixedRate.getDuration() != null ? fixedRate.getDuration().toMillis() : 0;
        long initialDelayMs = initialDelay != null && initialDelay.getDuration() != null ? initialDelay.getDuration().toMillis() : 0;


        // TODO: use a polymorphic factory
        if (cron != null) {
            return new CronTrigger(new JobExec(job, params), triggerName, cron);
        } else if (fixedDelayMs > 0) {
            return new FixedDelayTrigger(new JobExec(job, params), triggerName, fixedDelayMs, initialDelayMs);
        } else if (fixedRateMs > 0) {
            return new FixedRateTrigger(new JobExec(job, params), triggerName, fixedRateMs, initialDelayMs);
        }

        throw new IllegalStateException("Trigger must have either cron or fixed rate or fixed delay configured");
    }

    @BQConfigProperty("Job that the trigger applies to.")
    public void setJob(String jobName) {
        this.job = jobName;
    }

    @BQConfigProperty("Unique identifier, used in logging and reporting.")
    public void setTrigger(String triggerName) {
        this.trigger = triggerName;
    }

    @BQConfigProperty("Cron expression.")
    public void setCron(Cron cronExpression) {
        this.cron = cronExpression;
    }

    @BQConfigProperty("Delay between job executions in some time units." +
            " New job executions will be scheduled to run in D units after the completion of the preceding instance.")
    public void setFixedDelay(Duration fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    @BQConfigProperty("Fixed rate in some time units. New job instances will be run exactly every R units.")
    public void setFixedRate(Duration fixedRate) {
        this.fixedRate = fixedRate;
    }

    @BQConfigProperty("Initial delay in some time units. Applies to periodic and fixed-rate triggers.")
    public void setInitialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
    }

    @BQConfigProperty("Optional job parameters specific to this trigger")
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
