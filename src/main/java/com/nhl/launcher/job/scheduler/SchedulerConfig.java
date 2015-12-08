package com.nhl.launcher.job.scheduler;

import java.util.ArrayList;
import java.util.Collection;

public class SchedulerConfig {

	private String jobPropertiesPrefix;
	private Collection<TriggerDescriptor> triggers;
	private int threadPoolSize;
	private boolean clusteredLocks;

	public SchedulerConfig() {
		this.triggers = new ArrayList<>();
		this.threadPoolSize = 3;
	}

	public Collection<TriggerDescriptor> getTriggers() {
		return triggers;
	}

	public void setTriggers(Collection<TriggerDescriptor> triggers) {
		this.triggers = triggers;
	}

	public String getJobPropertiesPrefix() {
		return jobPropertiesPrefix;
	}

	public void setJobPropertiesPrefix(String propertiesNamespace) {
		this.jobPropertiesPrefix = propertiesNamespace;
	}

	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public boolean isClusteredLocks() {
		return clusteredLocks;
	}

	public void setClusteredLocks(boolean clusteredLocks) {
		this.clusteredLocks = clusteredLocks;
	}
}
