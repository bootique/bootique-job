package io.bootique.job.scheduler.execution;

import java.util.Set;

/**
 * @since 0.13
 */
public interface ExecutionFactory {

    Set<String> getAvailableJobs();

    Execution getExecution(String jobName);
}
