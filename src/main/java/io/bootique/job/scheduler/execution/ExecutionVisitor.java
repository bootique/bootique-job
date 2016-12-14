package io.bootique.job.scheduler.execution;

import java.util.Collection;

public interface ExecutionVisitor {

    void visitExecutionStep(Collection<SingleJobExecution> jobExecutions);
}
