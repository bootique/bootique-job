package io.bootique.job.scheduler.execution;

import java.util.Set;

/**
 * @since 0.13
 */
public interface ExecutionVisitor {

    void visitExecutionStep(Set<JobExecution> jobExecutions);
}
