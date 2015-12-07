package com.nhl.launcher.job.runnable;

import java.util.Map;

import com.nhl.launcher.job.Job;

public interface RunnableJobFactory {

	/**
	 * Creates a {@link RunnableJob} object that combines job instance with a
	 * set of parameters.
	 * 
	 * @param job
	 *            A job instance to run when the returned {@link RunnableJob} is
	 *            executed.
	 * @param parameters
	 *            A set of parameters to apply to the job when the returned
	 *            {@link RunnableJob} is executed.
	 * @return a wrapper around a job and a set of parameters.
	 */
	RunnableJob runnable(Job job, Map<String, Object> parameters);
}
