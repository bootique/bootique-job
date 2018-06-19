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

import java.util.UUID;

@BQConfig("Trigger of one of the following flavors: cron, periodic, fixed-rate.")
public class TriggerDescriptor {

	private String job;
	private String trigger;

	private String cron;
	private long fixedDelayMs;
	private long fixedRateMs;
	private long initialDelayMs;

	public TriggerDescriptor() {
		this.trigger = UUID.randomUUID().toString().replace("-", ""); // 32 chars
		this.initialDelayMs = 10 * 1000;
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

	public String getCron() {
		return cron;
	}

	@BQConfigProperty("Cron expression.")
	public void setCron(String cronExpression) {
		this.cron = cronExpression;
	}

	public long getFixedDelayMs() {
		return fixedDelayMs;
	}

	@BQConfigProperty("Delay between job executions in millis." +
			" New job instances will be scheduled to run in D milliseconds after the completion of the preceding instance.")
	public void setFixedDelayMs(long fixedDelayMs) {
		this.fixedDelayMs = fixedDelayMs;
	}

	public long getFixedRateMs() {
		return fixedRateMs;
	}

	@BQConfigProperty("Fixed rate in millis. New job instances will be run exactly every R milliseconds.")
	public void setFixedRateMs(long fixedRateMs) {
		this.fixedRateMs = fixedRateMs;
	}

	public long getInitialDelayMs() {
		return initialDelayMs;
	}

	@BQConfigProperty("Initial delay in millis. Applies to periodic and fixed-rate triggers.")
	public void setInitialDelayMs(long initialDelayMs) {
		this.initialDelayMs = initialDelayMs;
	}

	/**
	 * Returns a human-readable String with trigger parameters description. Used
	 * mainly for debugging.
	 * 
	 * @return A human-readable String with trigger parameters description.
	 */
	public String describeTrigger() {
		if (cron != null) {
			return "cron: " + cron;
		} else if (fixedDelayMs > 0) {
			return "fixedDelayMs: " + fixedDelayMs;
		} else if (fixedRateMs > 0) {
			return "fixedRateMs: " + fixedRateMs;
		} else {
			return "no trigger";
		}
	}

}
