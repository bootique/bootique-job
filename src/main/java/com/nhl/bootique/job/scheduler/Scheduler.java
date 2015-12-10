package com.nhl.bootique.job.scheduler;

import com.nhl.bootique.job.runnable.JobFuture;

public interface Scheduler {
	
	JobFuture runOnce(String jobName);

	int start();
}
