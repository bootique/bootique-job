package io.bootique.job.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

import java.util.Collections;
import java.util.Map;

/**
 * @since 0.13
 */
@BQConfig("Job group. Aggregates a number of jobs and job groups, that should be run together, possibly depending on each other." +
        " Provides the means to alter the \"default\" job configuration" +
        " (override parameters, that were specified in the job definition; add new parameters;" +
        " and also override the list of job's dependencies).")
@JsonTypeName("group")
public class JobGroupDefinition implements JobDefinition {

    private Map<String, SingleJobDefinition> jobs;

    public JobGroupDefinition() {
        this.jobs = Collections.emptyMap();
    }

    public Map<String, SingleJobDefinition> getJobs() {
        return jobs;
    }

    @BQConfigProperty("Jobs and job groups, that belong to this job group." +
            " Overriding of default parameters and dependencies can be done here.")
    public void setJobs(Map<String, SingleJobDefinition> jobs) {
        this.jobs = jobs;
    }
}
