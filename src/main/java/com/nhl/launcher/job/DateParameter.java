package com.nhl.launcher.job;

import java.time.LocalDate;

public class DateParameter extends JobParameterMetadata<LocalDate> {

	static LocalDate parse(String value) {
		return value != null ? LocalDate.parse(value) : null;
	}

	public DateParameter(String name, String defaultValue) {
		this(name, parse(defaultValue));
	}
	
	public DateParameter(String name, LocalDate defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected LocalDate parseValue(String value) {
		return DateParameter.parse(value);
	}
	
	@Override
	public String getTypeName() {
		return "date";
	}
}
