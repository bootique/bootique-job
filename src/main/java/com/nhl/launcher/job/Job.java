package com.nhl.launcher.job;

import java.util.Map;

import com.nhl.launcher.job.runnable.JobResult;

public interface Job {

	JobMetadata getMetadata();

	JobResult run(Map<String, Object> parameters);
}
