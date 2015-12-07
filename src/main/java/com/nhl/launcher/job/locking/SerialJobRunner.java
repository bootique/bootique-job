package com.nhl.launcher.job.locking;

import com.nhl.launcher.job.JobMetadata;
import com.nhl.launcher.job.runnable.JobResult;
import com.nhl.launcher.job.runnable.RunnableJob;

public interface SerialJobRunner {

	JobResult runSerially(RunnableJob executable, JobMetadata metadata);
}
