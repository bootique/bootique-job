package io.bootique.job;

import java.util.Set;

/**
 * @since 0.13
 */
public interface JobRegistry {

    Set<String> getAvailableJobs();

    Job getJob(String jobName);

    boolean allowsSimlutaneousExecutions(String jobName);
}
