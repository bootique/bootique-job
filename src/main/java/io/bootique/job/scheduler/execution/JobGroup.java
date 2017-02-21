package io.bootique.job.scheduler.execution;

import io.bootique.job.Job;
import io.bootique.job.JobListener;
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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class JobGroup implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobGroup.class);

    private volatile Job delegate;
    private Supplier<Job> delegateSupplier;
    private final Object lock;

    private String name;
    private Collection<Job> jobs;
    private DependencyGraph graph;
    private Scheduler scheduler;
    private Set<JobListener> listeners;

    public JobGroup(String name, Collection<Job> jobs, DependencyGraph graph, Scheduler scheduler, Set<JobListener> listeners) {
        this.name = name;
        this.jobs = jobs;
        this.delegateSupplier = this::buildDelegate;
        this.lock = new Object();

        this.graph = graph;
        this.scheduler = scheduler;
        this.listeners = listeners;
    }

    private Job buildDelegate() {
        Map<String, Job> jobMap = mapJobs(jobs);
        JobMetadata.Builder builder = JobMetadata.builder(name);
        for (Job job : jobMap.values()) {
            job.getMetadata().getParameters().forEach(builder::param);
        }
        JobMetadata metadata = builder.build();

        return new Job() {
            @Override
            public JobMetadata getMetadata() {
                return metadata;
            }

            @Override
            public JobResult run(Map<String, Object> parameters) {
                traverseExecution(jobExecutions -> {
                    Set<JobResult> results = execute(jobExecutions, jobMap);
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
            }
        };
    }

    private Map<String, Job> mapJobs(Collection<Job> jobs) {
        return jobs.stream().collect(Collectors.toMap(job -> job.getMetadata().getName(), job -> job));
    }

    private Job getDelegate() {
        if (delegate == null) {
            synchronized (lock) {
                if (delegate == null) {
                    delegate = delegateSupplier.get();
                }
            }
        }
        return delegate;
    }

    @Override
    public JobMetadata getMetadata() {
        return getDelegate().getMetadata();
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        // TODO: merge execution params into individual jobs' params
        return Callback.runAndNotify(getDelegate(), params, listeners);
    }

    private void traverseExecution(Consumer<Set<JobExecution>> visitor) {
        List<Set<JobExecution>> executions = graph.topSort();
        Collections.reverse(executions);
        executions.forEach(visitor::accept);
    }

    private Set<JobResult> execute(Set<JobExecution> jobExecutions, Map<String, Job> jobs) {
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
