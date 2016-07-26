package io.bootique.job.lock;

import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.RunnableJob;

public interface LockHandler {

	RunnableJob lockingJob(RunnableJob executable, JobMetadata metadata);
}
