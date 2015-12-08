package com.nhl.launcher.job.scheduler;

import com.nhl.launcher.job.runnable.JobFuture;

public interface Scheduler {
	
	JobFuture runOnce(String jobName);

	int start();
}
