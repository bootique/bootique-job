package io.bootique.job;

import java.util.Map;

import io.bootique.job.runnable.JobResult;

public interface Job {

	JobMetadata getMetadata();

	JobResult run(Map<String, Object> parameters);
}
