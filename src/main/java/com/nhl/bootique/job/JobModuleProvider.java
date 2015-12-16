package com.nhl.bootique.job;

import com.google.inject.Module;
import com.nhl.bootique.BQModuleProvider;
import com.nhl.bootique.job.runtime.JobModule;

public class JobModuleProvider implements BQModuleProvider{

	@Override
	public Module module() {
		return new JobModule();
	}
}
