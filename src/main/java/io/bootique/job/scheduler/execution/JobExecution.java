package io.bootique.job.scheduler.execution;

import java.util.Map;

/**
 * @since 0.13
 */
class JobExecution {

    private String jobName;
    private Map<String, Object> params;

    public JobExecution(String jobName, Map<String, Object> params) {
        this.jobName = jobName;
        this.params = params;
    }

    public String getJobName() {
        return jobName;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
