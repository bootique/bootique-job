package io.bootique.job;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class JobMetadata {

    private String name;
    private Collection<JobParameterMetadata<?>> parameters;

    JobMetadata(String name, Collection<JobParameterMetadata<?>> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    /**
     * A shortcut to build JobMetadata without parameters.
     *
     * @param name Job name
     * @return newly created {@link JobMetadata}.
     * @since 0.9
     */
    public static JobMetadata build(String name) {
        return builder(name).build();
    }

    /**
     * A shortcut to build JobMetadata without parameters.
     *
     * @param jobType A class that implements {@link Job}.
     * @return newly created {@link JobMetadata}.
     * @since 0.9
     */
    public static JobMetadata build(Class<?> jobType) {
        return builder(jobType).build();
    }

    /**
     * @param name symbolic name of the job.
     * @return a new instance of a metadata builder.
     * @since 0.9
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * @param jobType Java class of a job in question. Used to generate a default name for the job.
     * @return a new instance of a metadata builder.
     * @since 0.9
     */
    public static Builder builder(Class<?> jobType) {
        return builder(toName(jobType));
    }

    private static String toName(Class<?> jobType) {
        String name = jobType.getSimpleName().toLowerCase();
        return name.length() > "job".length() && name.endsWith("job")
                ? name.substring(0, name.length() - "job".length()) : name;
    }

    public String getName() {
        return name;
    }

    public Collection<JobParameterMetadata<?>> getParameters() {
        return parameters != null ? parameters : Collections.emptyList();
    }

    public static class Builder {

        private String name;
        private Collection<JobParameterMetadata<?>> parameters;

        private Builder(String name) {
            this.name = name;
            this.parameters = new ArrayList<>();
        }

        public Builder param(JobParameterMetadata<?> param) {
            this.parameters.add(param);
            return this;
        }

        public Builder stringParam(String name) {
            return stringParam(name, null);
        }

        public Builder stringParam(String name, String defaultValue) {
            return param(new StringParameter(name, defaultValue));
        }

        public Builder dateParam(String name) {
            return param(new DateParameter(name, (LocalDate) null));
        }

        public Builder dateParam(String name, String isoDate) {
            return param(new DateParameter(name, isoDate));
        }

        public Builder dateParam(String name, LocalDate date) {
            return param(new DateParameter(name, date));
        }

        public Builder longParam(String name) {
            return longParam(name, (Long) null);
        }

        public Builder longParam(String name, String longValue) {
            return param(new LongParameter(name, longValue));
        }

        public Builder longParam(String name, Long longValue) {
            return param(new LongParameter(name, longValue));
        }

        public JobMetadata build() {

            if (name == null) {
                throw new IllegalStateException("Job name is not configured");
            }

            return new JobMetadata(name, parameters);
        }

    }
}
