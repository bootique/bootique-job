package io.bootique.job;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @deprecated since 0.9 in favor of {@link JobMetadata#builder(Class)} and
 *             friends.
 */
public class JobMetadataBuilder {

	private String name;
	private Collection<JobParameterMetadata<?>> parameters;

	private static String toName(Class<?> jobType) {
		String name = jobType.getSimpleName().toLowerCase();
		return name.length() > "job".length() && name.endsWith("job")
				? name.substring(0, name.length() - "job".length()) : name;
	}

	/**
	 * A shortcut to build JobMetadata without parameters.
	 * 
	 * @param name
	 *            Job name
	 * @return newly created {@link JobMetadata}.
	 */
	public static JobMetadata build(String name) {
		return newBuilder(name).build();
	}

	/**
	 * A shortcut to build JobMetadata without parameters.
	 * 
	 * @param jobType
	 *            A class that implements {@link Job}.
	 * @return newly created {@link JobMetadata}.
	 */
	public static JobMetadata build(Class<?> jobType) {
		return newBuilder(jobType).build();
	}

	public static JobMetadataBuilder newBuilder(String name) {
		return new JobMetadataBuilder(name);
	}

	public static JobMetadataBuilder newBuilder(Class<?> jobType) {
		return newBuilder(toName(jobType));
	}

	JobMetadataBuilder(String name) {
		this.name = name;
		this.parameters = new ArrayList<>();
	}

	public JobMetadataBuilder param(JobParameterMetadata<?> param) {
		this.parameters.add(param);
		return this;
	}

	public JobMetadataBuilder stringParam(String name) {
		return stringParam(name, null);
	}

	public JobMetadataBuilder stringParam(String name, String defaultValue) {
		return param(new StringParameter(name, defaultValue));
	}

	public JobMetadataBuilder dateParam(String name) {
		return param(new DateParameter(name, (LocalDate) null));
	}

	public JobMetadataBuilder dateParam(String name, String isoDate) {
		return param(new DateParameter(name, isoDate));
	}

	public JobMetadataBuilder dateParam(String name, LocalDate date) {
		return param(new DateParameter(name, date));
	}

	public JobMetadataBuilder longParam(String name) {
		return longParam(name, (Long) null);
	}

	public JobMetadataBuilder longParam(String name, String longValue) {
		return param(new LongParameter(name, longValue));
	}

	public JobMetadataBuilder longParam(String name, Long longValue) {
		return param(new LongParameter(name, longValue));
	}

	public JobMetadata build() {

		if (name == null) {
			throw new IllegalStateException("Job name is not configured");
		}

		return new JobMetadata(name, parameters);
	}
}
