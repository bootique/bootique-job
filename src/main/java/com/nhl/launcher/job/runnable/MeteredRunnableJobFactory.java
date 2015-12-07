package com.nhl.launcher.job.runnable;

import java.util.Map;

import org.springframework.boot.actuate.metrics.CounterService;

import com.nhl.launcher.job.Job;

public class MeteredRunnableJobFactory implements RunnableJobFactory {

	private static final String ACTIVE_ALL_METRIC = "scheduler.jobs.active";
	private static final String ACTIVE_SPECIFIC_METRIC = "scheduler.jobs.%s.active";

	private static final String EXECUTED_ALL_METRIC = "scheduler.jobs.executed";
	private static final String EXECUTED_SPECIFIC_METRIC = "scheduler.jobs.%s.executed";

	private RunnableJobFactory delegate;
	private CounterService counter;

	public MeteredRunnableJobFactory(RunnableJobFactory delegate, CounterService counter) {
		this.delegate = delegate;
		this.counter = counter;
	}

	@Override
	public RunnableJob runnable(Job job, Map<String, Object> parameters) {
		RunnableJob rj = delegate.runnable(job, parameters);

		return () -> {

			String activeSpecificName = String.format(ACTIVE_SPECIFIC_METRIC, job.getMetadata().getName());
			counter.increment(ACTIVE_ALL_METRIC);
			counter.increment(activeSpecificName);

			try {
				// TODO: add metrics by result
				return rj.run();
			} finally {
				counter.decrement(ACTIVE_ALL_METRIC);
				counter.decrement(activeSpecificName);

				counter.increment(EXECUTED_ALL_METRIC);
				counter.increment(String.format(EXECUTED_SPECIFIC_METRIC, job.getMetadata().getName()));
			}
		};
	}
}
