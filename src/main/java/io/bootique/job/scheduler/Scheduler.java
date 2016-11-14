package io.bootique.job.scheduler;

import io.bootique.job.runnable.JobFuture;

import java.util.Map;

public interface Scheduler {
	
	JobFuture runOnce(String jobName);

	JobFuture runOnce(String jobName, Map<String, Object> parameters);

	int start();
}
