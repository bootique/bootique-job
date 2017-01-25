package io.bootique.job;

import io.bootique.job.runnable.JobResult;

import java.util.Map;
import java.util.function.Consumer;

public interface JobListener {

    void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> callback);
}
