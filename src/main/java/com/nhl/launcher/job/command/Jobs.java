package com.nhl.launcher.job.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.cayenne.di.Module;

import com.nhl.launcher.LauncherUtil;
import com.nhl.launcher.job.Job;
import com.nhl.launcher.job.locking.LocalSerialJobRunner;
import com.nhl.launcher.job.locking.SerialJobRunner;
import com.nhl.launcher.job.scheduler.DefaultScheduler;
import com.nhl.launcher.job.scheduler.Scheduler;

public class Jobs {

	private Collection<Class<? extends Job>> jobTypes;

	/**
	 * A DI key for the collection of available jobs.
	 */
	public static final String JOBS_COLLECTION_KEY = "jobs";

	public static Jobs jobs() {
		return new Jobs();
	}

	private Jobs() {
		this.jobTypes = new HashSet<>();
	}

	@SafeVarargs
	public final Jobs addJobs(Class<? extends Job>... jobTypes) {
		Arrays.asList(jobTypes).forEach(jt -> this.jobTypes.add(jt));
		return this;
	}

	public Module module() {
		return binder -> {

			LauncherUtil.bindCommand(binder, ExecCommand.class);
			LauncherUtil.bindCommand(binder, ListCommand.class);

			jobTypes.forEach(jt -> binder.<Job> bindList(JOBS_COLLECTION_KEY).add(jt));

			binder.bind(SerialJobRunner.class).to(LocalSerialJobRunner.class);
			binder.bind(Scheduler.class).to(DefaultScheduler.class);
		};
	}

}
