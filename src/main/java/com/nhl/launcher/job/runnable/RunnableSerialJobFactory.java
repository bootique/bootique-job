package com.nhl.launcher.job.runnable;

import java.util.Map;

import com.nhl.launcher.job.Job;
import com.nhl.launcher.job.SerialJob;
import com.nhl.launcher.job.locking.SerialJobRunner;

public class RunnableSerialJobFactory implements RunnableJobFactory {

	private RunnableJobFactory delegate;
	private SerialJobRunner serialJobRunner;

	public RunnableSerialJobFactory(RunnableJobFactory delegate, SerialJobRunner serialJobRunner) {
		this.delegate = delegate;
		this.serialJobRunner = serialJobRunner;
	}

	@Override
	public RunnableJob runnable(Job job, Map<String, Object> parameters) {

		RunnableJob rj = delegate.runnable(job, parameters);
		boolean serialConstraint = job.getClass().getAnnotation(SerialJob.class) != null;

		return serialConstraint ? () -> {
			return serialJobRunner.runSerially(rj, job.getMetadata());
		} : rj;
	}

}
