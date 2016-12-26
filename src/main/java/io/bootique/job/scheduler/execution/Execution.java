package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;

/**
 * @since 0.13
 */
public interface Execution extends Job {

    void traverseExecution(ExecutionVisitor visitor);
}
