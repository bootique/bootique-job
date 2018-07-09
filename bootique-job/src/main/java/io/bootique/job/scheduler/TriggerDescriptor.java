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
import io.bootique.value.Duration;

import java.util.UUID;

@BQConfig("Trigger of one of the following flavors: cron, periodic, fixed-rate.")
public class TriggerDescriptor {

	private String job;
	private String trigger;

	private Cron cron;
	private Duration fixedDelay;
	private Duration fixedRate;
	private Duration initialDelay;

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
	 * @deprecated since 0.26 use {@link #getFixedDelay()} constructor.
	 */
	@Deprecated
	public long getFixedDelayMs() {
		if (fixedDelay != null && fixedDelay.getDuration() != null) {
			return fixedDelay.getDuration().toMillis();
		}
		return 0;
	}

	/**
	 * @deprecated since 0.26 use {@link #setFixedDelay(Duration)} constructor.
	 */
	@Deprecated
	@BQConfigProperty("deprecated, Long, internally converted to Duration.")
	public void setFixedDelayMs(long fixedDelayMs) {
		this.fixedDelay = new Duration(fixedDelayMs);
	}

	/**
	 * @deprecated since 0.26 use {@link #getFixedRate()} constructor.
	 */
	@Deprecated
	public long getFixedRateMs() {
		if (fixedRate != null && fixedRate.getDuration() != null) {
			return fixedRate.getDuration().toMillis();
		}
		return 0;
	}

	/**
	 * @deprecated since 0.26 use {@link #setFixedRate(Duration)} constructor.
	 */
	@Deprecated
	@BQConfigProperty("deprecated, Long, internally converted to Duration.")
	public void setFixedRateMs(long fixedRateMs) {
		this.fixedRate = new Duration(fixedRateMs);
	}

	/**
	 * @deprecated since 0.26 use {@link #getInitialDelay()} constructor.
	 */
	@Deprecated
	public long getInitialDelayMs() {
		if (initialDelay != null && initialDelay.getDuration() != null) {
			return initialDelay.getDuration().toMillis();
		}
		return 0;
	}

	/**
	 * @deprecated since 0.26 use {@link #setInitialDelay(Duration)} constructor.
	 */
	@Deprecated
	@BQConfigProperty("deprecated, Long, internally converted to Duration.")
	public void setInitialDelayMs(long initialDelayMs) {
		this.initialDelay = new Duration(initialDelayMs);
	}

	public Duration getFixedDelay() {
		return fixedDelay;
	}

	@BQConfigProperty("Duration String.")
	public void setFixedDelay(Duration fixedDelay) {
		this.fixedDelay = fixedDelay;
	}

	public Duration getFixedRate() {
		return fixedRate;
	}

	@BQConfigProperty("Duration String.")
	public void setFixedRate(Duration fixedRate) {
		this.fixedRate = fixedRate;
	}

	public Duration getInitialDelay() {
		return initialDelay;
	}

	@BQConfigProperty("Duration String.")
	public void setInitialDelay(Duration initialDelay) {
		this.initialDelay = initialDelay;
	}

	/**
	 * Returns a human-readable String with trigger parameters description. Used
	 * mainly for debugging.
	 * 
	 * @return A human-readable String with trigger parameters description.
	 */
	public String describeTrigger() {
		if (cron != null && cron.getExpression() != null) {
			return "cron: " + cron.getExpression();
		} else if (fixedDelay != null && fixedDelay.getDuration() != null && fixedDelay.getDuration().toMillis() > 0) {
			return "fixedDelay" + fixedDelay.getDuration().toMillis();
		} else if(fixedRate != null && fixedRate.getDuration() != null && fixedRate.getDuration().toMillis() > 0) {
			return "fixedRate" + fixedRate.getDuration().toMillis();
		} else {
			return "no trigger";
		}
	}
}
