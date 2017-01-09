package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class GroupExecution implements Execution {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupExecution.class);

    private String name;
    private Map<String, Job> jobs;
    private DependencyGraph graph;
    private Scheduler scheduler;

    public GroupExecution(String name, Collection<Job> jobs, DependencyGraph graph, Scheduler scheduler) {
        this.name = name;
        this.jobs = mapJobs(jobs);
        this.graph = graph;
        this.scheduler = scheduler;
    }

    private Map<String, Job> mapJobs(Collection<Job> jobs) {
        return jobs.stream().collect(Collectors.toMap(job -> job.getMetadata().getName(), job -> job));
    }

    @Override
    public JobMetadata getMetadata() {
        JobMetadata.Builder builder = JobMetadata.builder(name);
        for (Job job : jobs.values()) {
            job.getMetadata().getParameters().forEach(builder::param);
        }
        return builder.build();
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        // TODO: merge execution params into individual jobs' params
        try {
            traverseExecution(jobExecutions -> {
                Set<JobResult> results = execute(jobExecutions);
                results.forEach(result -> {
                    if (result.getOutcome() != JobOutcome.SUCCESS) {
                        String message = "Failed to execute job: " + result.getMetadata().getName();
                        if (result.getMessage() != null) {
                            message += ". Reason: " + result.getMessage();
                        }
                        throw new RuntimeException(message, result.getThrowable());
                    }
                });
            });
            return JobResult.success(getMetadata());
        } catch (Exception e) {
            return JobResult.failure(getMetadata(), e);
        }
    }

    @Override
    public void traverseExecution(ExecutionVisitor visitor) {
        List<Set<JobExecution>> executions = graph.topSort();
        Collections.reverse(executions);
        executions.forEach(visitor::visitExecutionStep);
    }

    private Set<JobResult> execute(Set<JobExecution> jobExecutions) {
        if (jobExecutions.isEmpty()) {
            JobResult.failure(getMetadata(), "No jobs");
        }

        List<JobFuture> futures = jobExecutions.stream()
                .map(jobExecution -> {
                    Job job = jobs.get(jobExecution.getJobName());
                    return scheduler.runOnce(job, jobExecution.getParams());
                })
                .collect(Collectors.toList());

        Set<JobResult> failures = new HashSet<>();
        futures.stream().map(JobFuture::get).forEach(r -> {
            if (r.getThrowable() == null) {
				LOGGER.info(String.format("Finished job '%s', result: %s, message: %s", r.getMetadata().getName(),
						r.getOutcome(), r.getMessage()));
			} else {
				LOGGER.error(String.format("Finished job '%s', result: %s, message: %s", r.getMetadata().getName(),
						r.getOutcome(), r.getMessage()), r.getThrowable());
			}
            if (r.getOutcome() != JobOutcome.SUCCESS) {
                failures.add(r);
                // TODO: cancel jobs from the current group
            }
        });
        return failures;
    }

}
