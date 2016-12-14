package io.bootique.job.scheduler.execution;

public interface Execution {

    void traverseExecution(ExecutionVisitor visitor);
}
