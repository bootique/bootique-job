package com.nhl.bootique.job.lock;

import com.nhl.bootique.job.JobMetadata;
import com.nhl.bootique.job.runnable.RunnableJob;

public interface LockHandler {

	RunnableJob lockingJob(RunnableJob executable, JobMetadata metadata);
}
