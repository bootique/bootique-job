package io.bootique.job.scheduler.execution;

import java.util.Set;

/**
 * @since 0.13
 */
interface ExecutionVisitor {

    void visitExecutionStep(Set<JobExecution> jobExecutions);
}
