package com.nhl.bootique.job.runnable;

import java.util.Map;

import com.nhl.bootique.job.Job;
import com.nhl.bootique.job.SerialJob;
import com.nhl.bootique.job.lock.LockHandler;

public class LockAwareRunnableJobFactory implements RunnableJobFactory {

	private RunnableJobFactory delegate;
	private LockHandler serialJobRunner;

	public LockAwareRunnableJobFactory(RunnableJobFactory delegate, LockHandler serialJobRunner) {
		this.delegate = delegate;
		this.serialJobRunner = serialJobRunner;
	}

	@Override
	public RunnableJob runnable(Job job, Map<String, Object> parameters) {

		RunnableJob rj = delegate.runnable(job, parameters);
		boolean serialConstraint = job.getClass().getAnnotation(SerialJob.class) != null;
		return serialConstraint ? serialJobRunner.lockingJob(rj, job.getMetadata()) : rj;
	}

}
