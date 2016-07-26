package io.bootique.job.scheduler;

import io.bootique.job.runnable.JobFuture;

public interface Scheduler {
	
	JobFuture runOnce(String jobName);

	int start();
}
