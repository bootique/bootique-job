package com.nhl.launcher.job;

public class StringParameter extends JobParameterMetadata<String> {

	public StringParameter(String name, String defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected String parseValue(String value) {
		return value;
	}

	@Override
	public String getTypeName() {
		return "string";
	}
}
