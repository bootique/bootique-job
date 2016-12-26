package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;

public interface Execution extends Job {

    void traverseExecution(ExecutionVisitor visitor);
}
