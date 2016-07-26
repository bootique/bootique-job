package io.bootique.job.runnable;

import java.util.Map;

import io.bootique.job.Job;
import io.bootique.job.SerialJob;
import io.bootique.job.lock.LockHandler;

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
