package com.nhl.bootique.job.runtime;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.job.Job;

/**
 * @deprecated since 0.9 in favor of {@link JobModule#contributeJobs(Binder)}.
 */
public class JobBinder {

	public static JobBinder contributeTo(Binder binder) {
		return new JobBinder(binder);
	}

	private Binder binder;

	JobBinder(Binder binder) {
		this.binder = binder;
	}

	@SafeVarargs
	public final void jobTypes(Class<? extends Job>... jobTypes) {
		Preconditions.checkNotNull(jobTypes);
		jobTypes(Arrays.asList(jobTypes));
	}

	public void jobTypes(Collection<Class<? extends Job>> jobTypes) {
		Multibinder<Job> jobBinder = Multibinder.newSetBinder(binder, Job.class);
		jobTypes.forEach(jt -> jobBinder.addBinding().to(jt).in(Singleton.class));
	}

}
