package io.bootique.job.config;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Collections;
import java.util.Map;

/**
 * @since 0.13
 */
@JsonTypeName("group")
public class JobGroup implements JobDefinition {

    private Map<String, SingleJob> jobs;

    public JobGroup() {
        this.jobs = Collections.emptyMap();
    }

    public Map<String, SingleJob> getJobs() {
        return jobs;
    }

    public void setJobs(Map<String, SingleJob> jobs) {
        this.jobs = jobs;
    }
}
