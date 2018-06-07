package io.bootique.job;

import java.util.Set;

/**
 * @since 0.13
 */
public interface JobRegistry {

    Set<String> getAvailableJobs();

    Job getJob(String jobName);

    /**
     * @deprecated since 0.26 use correctly named {@link #allowsSimultaneousExecutions(String)} method
     */
    @Deprecated
    boolean allowsSimlutaneousExecutions(String jobName);

    /**
     * @since 0.26
     */
    boolean allowsSimultaneousExecutions(String jobName);
}
