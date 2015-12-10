package com.nhl.bootique.job;

import java.util.Map;

import com.nhl.bootique.job.runnable.JobResult;

public interface Job {

	JobMetadata getMetadata();

	JobResult run(Map<String, Object> parameters);
}
