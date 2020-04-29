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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.job.value.Cron;
import io.bootique.value.Duration;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@BQConfig("Trigger of one of the following flavors: cron, periodic, fixed-rate.")
public class TriggerDescriptor {

    private String job;
    private String trigger;

    private Cron cron;
    private Duration fixedDelay;
    private Duration fixedRate;
    private Duration initialDelay;

    private Map<String, Object> params = Collections.emptyMap();

    public TriggerDescriptor() {
        this.trigger = UUID.randomUUID().toString().replace("-", ""); // 32 chars
        this.initialDelay = new Duration(10 * 1000);
    }

    public String getJob() {
        return job;
    }

    @BQConfigProperty("Job that the trigger applies to.")
    public void setJob(String jobName) {
        this.job = jobName;
    }

    public String getTrigger() {
        return trigger;
    }

    @BQConfigProperty("Unique identifier, used in logging and reporting.")
    public void setTrigger(String triggerName) {
        this.trigger = triggerName;
    }

    public Cron getCron() {
        return cron;
    }

    @BQConfigProperty("Cron expression.")
    public void setCron(Cron cronExpression) {
        this.cron = cronExpression;
    }

    /**
     * @deprecated since 1.0.RC1 use {@link #setFixedDelay(Duration)}.
     */
    @Deprecated
    @BQConfigProperty("deprecated, Long, internally converted to Duration.")
    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelay = new Duration(fixedDelayMs);
    }

    /**
     * @deprecated since 1.0.RC1 use {@link #setFixedRate(Duration)}.
     */
    @Deprecated
    @BQConfigProperty("deprecated, Long, internally converted to Duration.")
    public void setFixedRateMs(long fixedRateMs) {
        this.fixedRate = new Duration(fixedRateMs);
    }

    /**
     * @deprecated since 1.0.RC1 use {@link #setInitialDelay(Duration)}.
     */
    @Deprecated
    @BQConfigProperty("deprecated, Long, internally converted to Duration.")
    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelay = new Duration(initialDelayMs);
    }

    /**
     * @return a Duration corresponding to the fixed delay
     * @since 1.0.RC1
     */
    public Duration getFixedDelay() {
        return fixedDelay;
    }

    /**
     * @param fixedDelay
     * @since 1.0.RC1
     */
    @BQConfigProperty("Delay between job executions in some time units." +
            " New job executions will be scheduled to run in D units after the completion of the preceding instance.")
    public void setFixedDelay(Duration fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    /**
     * @return a Duration corresponding to the trigger "fixed rate".
     * @since 1.0.RC1
     */
    public Duration getFixedRate() {
        return fixedRate;
    }

    /**
     * @param fixedRate
     * @since 1.0.RC1
     */
    @BQConfigProperty("Fixed rate in some time units. New job instances will be run exactly every R units.")
    public void setFixedRate(Duration fixedRate) {
        this.fixedRate = fixedRate;
    }

    /**
     * @return a delay used before the trigger starts.
     * @since 1.0.RC1
     */
    public Duration getInitialDelay() {
        return initialDelay;
    }

    /**
     * @param initialDelay
     * @since 1.0.RC1
     */
    @BQConfigProperty("Initial delay in some time units. Applies to periodic and fixed-rate triggers.")
    public void setInitialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
    }

    /**
     * Returns a human-readable String with trigger parameters description. Used mainly for debugging.
     *
     * @return A human-readable String with trigger parameters description.
     */
    public String describeTrigger() {
        if (cron != null && cron.getExpression() != null) {
            return "cron: " + cron.getExpression();
        } else if (fixedDelay != null && fixedDelay.getDuration() != null && fixedDelay.getDuration().toMillis() > 0) {
            return "fixedDelay" + fixedDelay.getDuration().toMillis();
        } else if (fixedRate != null && fixedRate.getDuration() != null && fixedRate.getDuration().toMillis() > 0) {
            return "fixedRate" + fixedRate.getDuration().toMillis();
        } else {
            return "no trigger";
        }
    }

    public Map<String, Object> getParams() {
        return params;
    }

    @BQConfigProperty
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
