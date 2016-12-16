package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;

public interface Execution extends Job {

    String getName();

    void traverseExecution(ExecutionVisitor visitor);
}
