package com.nhl.launcher.job.lock;

import com.nhl.launcher.job.JobMetadata;
import com.nhl.launcher.job.runnable.RunnableJob;

public interface LockHandler {

	RunnableJob lockingJob(RunnableJob executable, JobMetadata metadata);
}
