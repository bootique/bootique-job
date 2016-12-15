package io.bootique.job.scheduler.execution;

import java.util.List;

public interface ExecutionVisitor {

    void visitExecutionStep(List<SingleJobExecution> jobExecutions);
}
