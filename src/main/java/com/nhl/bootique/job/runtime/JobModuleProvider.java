package com.nhl.bootique.job.runtime;

import com.google.inject.Module;
import com.nhl.bootique.BQModuleProvider;

public class JobModuleProvider implements BQModuleProvider{

	@Override
	public Module module() {
		return new JobModule();
	}
}
