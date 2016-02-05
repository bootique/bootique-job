package com.nhl.bootique.job.scheduler;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

public class TriggerDescriptor {

	private String job;
	private String trigger;

	private String cron;
	private long fixedDelayMs;
	private long fixedRateMs;
	private long initialDelayMs = 10 * 1000;

	public String getJob() {
		return job;
	}

	public void setJob(String jobName) {
		this.job = jobName;
	}

	public String getTrigger() {
		return trigger;
	}

	public void setTrigger(String triggerName) {
		this.trigger = triggerName;
	}

	public String getCron() {
		return cron;
	}

	public void setCron(String cronExpression) {
		this.cron = cronExpression;
	}

	public long getFixedDelayMs() {
		return fixedDelayMs;
	}

	public void setFixedDelayMs(long fixedDelayMs) {
		this.fixedDelayMs = fixedDelayMs;
	}

	public long getFixedRateMs() {
		return fixedRateMs;
	}

	public void setFixedRateMs(long fixedRateMs) {
		this.fixedRateMs = fixedRateMs;
	}

	public long getInitialDelayMs() {
		return initialDelayMs;
	}

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

	public Trigger createTrigger() {
		if (cron != null) {
			return new CronTrigger(cron);
		} else if (fixedDelayMs > 0) {
			PeriodicTrigger pt = new PeriodicTrigger(fixedDelayMs);
			pt.setFixedRate(false);
			pt.setInitialDelay(initialDelayMs);
			return pt;
		} else if (fixedRateMs > 0) {
			PeriodicTrigger pt = new PeriodicTrigger(fixedRateMs);
			pt.setFixedRate(true);
			pt.setInitialDelay(initialDelayMs);
			return pt;
		}

		throw new IllegalStateException(
				"Trigger is misconfigured. Either of 'cron', 'fixedDelayMs', 'fixedRateMs' must be set.");
	}

}
