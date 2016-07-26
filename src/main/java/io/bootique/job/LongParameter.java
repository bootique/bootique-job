package io.bootique.job;

public class LongParameter extends JobParameterMetadata<Long> {

	static Long parse(String value) {
		return value != null ? Long.valueOf(value) : null;
	}

	public LongParameter(String name, String defaultValue) {
		this(name, parse(defaultValue));
	}

	public LongParameter(String name, Long defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected Long parseValue(String value) {
		return LongParameter.parse(value);
	}

	@Override
	public String getTypeName() {
		return "long";
	}
}
