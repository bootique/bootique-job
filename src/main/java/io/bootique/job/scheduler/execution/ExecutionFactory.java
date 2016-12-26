package io.bootique.job.scheduler.execution;

import java.util.Set;

public interface ExecutionFactory {

    Set<String> getAvailableJobs();

    Execution getExecution(String jobName);
}
