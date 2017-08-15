package io.bootique.job.runnable;

import io.bootique.job.Job;
import io.bootique.job.JobRegistry;
import io.bootique.job.lock.LockHandler;

import java.util.Map;

public class LockAwareRunnableJobFactory implements RunnableJobFactory {

	private RunnableJobFactory delegate;
	private LockHandler serialJobRunner;
	private JobRegistry jobRegistry;

	public LockAwareRunnableJobFactory(RunnableJobFactory delegate,
									   LockHandler serialJobRunner,
									   JobRegistry jobRegistry) {
		this.delegate = delegate;
		this.serialJobRunner = serialJobRunner;
		this.jobRegistry = jobRegistry;
	}

	@Override
	public RunnableJob runnable(Job job, Map<String, Object> parameters) {

		RunnableJob rj = delegate.runnable(job, parameters);
		boolean allowsSimlutaneousExecutions = jobRegistry.allowsSimlutaneousExecutions(job.getMetadata().getName());
		return allowsSimlutaneousExecutions ? rj : serialJobRunner.lockingJob(rj, job.getMetadata());
	}

}
