package io.bootique.job.scheduler;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

@BQConfig("Trigger of a one of the following flavors: cron, periodic, fixed-rate.")
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
