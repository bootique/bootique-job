package io.bootique.job.scheduler;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.scheduler.execution.ExecutionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class LazyJobGroup implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(LazyJobGroup.class);

    private String groupName;
    private ExecutionFactory executionFactory;
    private DefaultScheduler scheduler;

    private volatile CombinedJob delegate;
    private final Object lock;

    LazyJobGroup(String groupName, ExecutionFactory executionFactory, DefaultScheduler scheduler) {
        this.groupName = groupName;
        this.executionFactory = executionFactory;
        this.scheduler = scheduler;
        this.lock = new Object();
    }

    @Override
    public JobMetadata getMetadata() {
		if (delegate == null) {
			// do not trigger graph traversal until job group is run
			// TODO: lazy metadata? :)
			return JobMetadata.build(groupName);
		}
        return delegate.getMetadata();
    }

    @Override
    public JobResult run(Map<String, Object> parameters) {
        return getDelegate().run(parameters);
    }

    private CombinedJob getDelegate() {
        if (delegate == null) {
            synchronized (lock) {
                if (delegate == null) {
                    delegate = createCombinedJob();
                }
            }
        }
        return delegate;
    }

    private CombinedJob createCombinedJob() {
        CombinedJob.Builder jobBuilder = CombinedJob.builder(JobMetadata.builder(groupName));

        executionFactory.getExecution(groupName).traverseExecution(jobExecutions -> {
            jobBuilder.thenRun(() -> {
				if (jobExecutions.isEmpty()) {
					return new JobFuture(new ExpiredFuture(),
							() -> JobResult.failure(getMetadata(), "No jobs"));
				}

                List<JobFuture> futures = jobExecutions.stream()
                        .map(job -> scheduler.runOnce(job.getJobName(), job.getParams()))
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

        return jobBuilder.build();
    }
}
