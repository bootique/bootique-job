package io.bootique.job;

public abstract class JobParameterMetadata<T> {

	private String name;
	private T defaultValue;

	public JobParameterMetadata(String name, T defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	public String getName() {
		return name;
	}

	public T fromString(String stringValue) {
		return stringValue != null ? parseValue(stringValue) : defaultValue;
	}
	
	public T getDefaultValue() {
		return defaultValue;
	}

	protected abstract T parseValue(String value);
	
	public abstract String getTypeName();
}
