package com.nhl.bootique.job;

import java.util.Collection;
import java.util.Collections;

public class JobMetadata {

	private String name;
	private Collection<JobParameterMetadata<?>> parameters;

	public JobMetadata(String name, Collection<JobParameterMetadata<?>> parameters) {
		this.name = name;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public Collection<JobParameterMetadata<?>> getParameters() {
		return parameters != null ? parameters : Collections.emptyList();
	}
}
