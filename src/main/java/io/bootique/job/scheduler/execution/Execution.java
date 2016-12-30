package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.DefaultScheduler;
import io.bootique.job.scheduler.ExpiredFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Execution implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(Execution.class);

    private String name;
    private Map<String, Job> jobs;
    private DependencyGraph graph;
    private DefaultScheduler scheduler;

    public Execution(String name, Collection<Job> jobs, DependencyGraph graph, DefaultScheduler scheduler) {
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
        CombinedJob.Builder jobBuilder = CombinedJob.builder(getMetadata());
        traverseExecution(jobExecutions -> {
            jobBuilder.thenRun(() -> {
				if (jobExecutions.isEmpty()) {
					return new JobFuture(new ExpiredFuture(),
							() -> JobResult.failure(getMetadata(), "No jobs"));
				}

                List<JobFuture> futures = jobExecutions.stream()
                        .map(jobExecution -> {
                            Job job = jobs.get(jobExecution.getJobName());
                            return scheduler.runOnce(job, jobExecution.getParams(), job.getMetadata(), new Date());
                        })
                        .collect(Collectors.toList());

                List<JobResult> failures = new ArrayList<>();
                futures.stream().map(JobFuture::get).forEach(r -> {
                    if (r.getMessage() != null) {
                        LOGGER.info(String.format("Finished job '%s', result: %s, message: %s", r.getMetadata().getName(),
                                r.getOutcome(), r.getMessage()));
                    } else {
                        LOGGER.info(String.format("Finished job '%s', result: %s", r.getMetadata().getName(), r.getOutcome()));
                    }
                    if (r.getOutcome() == JobOutcome.FAILURE) {
                        failures.add(r);
                        // TODO: cancel jobs from the current group
                    }
                });
                // TODO: provide description of failures
                return new JobFuture(new ExpiredFuture(),
                        () -> failures.isEmpty() ? JobResult.success(getMetadata()) : JobResult.failure(getMetadata()));
            });
        });
        return jobBuilder.build().run(params);
    }

    private void traverseExecution(ExecutionVisitor visitor) {
        List<Set<SingleJobExecution>> executions = graph.topSort();
        Collections.reverse(executions);
        executions.forEach(visitor::visitExecutionStep);
    }

    private interface ExecutionVisitor {
        void visitExecutionStep(Set<SingleJobExecution> jobExecutions);
    }
}
