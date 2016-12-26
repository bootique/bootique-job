package io.bootique.job.scheduler.execution;

import java.util.Set;

public interface ExecutionVisitor {

    void visitExecutionStep(Set<JobExecution> jobExecutions);
}
