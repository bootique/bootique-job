package io.bootique.job.config;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Collections;
import java.util.Map;

/**
 * @since 0.13
 */
@JsonTypeName("group")
public class JobGroupDefinition implements JobDefinition {

    private Map<String, SingleJobDefinition> jobs;

    public JobGroupDefinition() {
        this.jobs = Collections.emptyMap();
    }

    public Map<String, SingleJobDefinition> getJobs() {
        return jobs;
    }

    public void setJobs(Map<String, SingleJobDefinition> jobs) {
        this.jobs = jobs;
    }
}
