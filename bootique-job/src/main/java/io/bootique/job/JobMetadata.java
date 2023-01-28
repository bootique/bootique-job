/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.job;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class JobMetadata {

    private final String name;
    private final Collection<JobParameterMetadata<?>> parameters;
    private final String lockName;
    private final boolean group;
    private final boolean serial;
    private final Set<String> dependsOn;

    protected JobMetadata(
            String name,
            Collection<JobParameterMetadata<?>> parameters,
            String lockName,
            Set<String> dependsOn,
            boolean group,
            boolean serial) {
        this.name = name;
        this.lockName = lockName;
        this.parameters = parameters;
        this.dependsOn = dependsOn;
        this.group = group;
        this.serial = serial;
    }

    /**
     * A shortcut to build JobMetadata without parameters.
     *
     * @param name Job name
     * @return newly created {@link JobMetadata}.
     */
    public static JobMetadata build(String name) {
        return builder(name).build();
    }

    /**
     * A shortcut to build JobMetadata without parameters.
     *
     * @param jobType A class that implements {@link Job}.
     * @return newly created {@link JobMetadata}.
     */
    public static JobMetadata build(Class<?> jobType) {
        return builder(jobType).build();
    }

    /**
     * @param name symbolic name of the job.
     * @return a new instance of a metadata builder.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * @param jobType Java class of a job in question. Used to generate a default name for the job.
     * @return a new instance of a metadata builder.
     */
    public static Builder builder(Class<?> jobType) {
        return builder(toName(jobType)).serial(isSerial(jobType));
    }

    private static boolean isSerial(Class<?> jobType) {
        return jobType.isAnnotationPresent(SerialJob.class);
    }

    private static String toName(Class<?> jobType) {
        String name = jobType.getSimpleName().toLowerCase();
        return name.length() > "job".length() && name.endsWith("job")
                ? name.substring(0, name.length() - "job".length()) : name;
    }

    public String getName() {
        return name;
    }

    /**
     * @return lock name to use with this job
     * @since 3.0
     */
    public String getLockName() {
        return lockName;
    }

    public Collection<JobParameterMetadata<?>> getParameters() {
        return parameters != null ? parameters : Collections.emptyList();
    }

    /**
     * @since 3.0
     */
    public Set<String> getDependsOn() {
        return dependsOn;
    }

    /**
     * @since 3.0
     */
    public boolean isSerial() {
        return serial;
    }

    /**
     * @since 3.0
     */
    public boolean isGroup() {
        return group;
    }

    /**
     * Creates and returns a map with parameters derived from "rawParams", where values of all known parameters are
     * converted according to Job parameter metadata policies.
     *
     * @since 3.0
     */
    public Map<String, Object> convertParameters(Map<String, ?> rawParams) {

        Collection<JobParameterMetadata<?>> paramsMd = getParameters();
        if (paramsMd.isEmpty()) {
            return rawParams != null ? (Map<String, Object>) rawParams : Collections.emptyMap();
        }

        // merge custom parameters with converted values (custom or default) of declared parameters
        Map<String, Object> converted = rawParams != null ? new HashMap<>(rawParams) : new HashMap<>();
        for (JobParameterMetadata<?> param : paramsMd) {
            Object rawVal = rawParams.get(param.getName());
            Object value = param.fromString(rawVal != null ? rawVal.toString() : null);
            converted.put(param.getName(), value);
        }

        return converted;
    }

    public static class Builder {

        static Integer parseInt(String value) {
            return value != null ? Integer.valueOf(value) : null;
        }

        static Long parseLong(String value) {
            return value != null ? Long.valueOf(value) : null;
        }

        static LocalDate parseDate(String value) {
            return value != null ? LocalDate.parse(value) : null;
        }

        static LocalDateTime parseDateTime(String value) {
            return value != null ? LocalDateTime.parse(value) : null;
        }

        private final String name;
        private final Collection<JobParameterMetadata<?>> parameters;
        private final Set<String> dependsOn;
        private boolean serial;
        private boolean group;
        private String lockName;

        private Builder(String name) {
            this.name = name;
            this.parameters = new ArrayList<>();
            this.dependsOn = new LinkedHashSet<>();
        }

        /**
         * Syntactic sugar that allows e.g. to load multiple dependencies from a collection in a fluent style
         *
         * @since 3.0
         */
        public Builder config(Consumer<Builder> configurator) {
            configurator.accept(this);
            return this;
        }

        /**
         * @since 3.0
         */
        public Builder group(boolean group) {
            this.group = group;
            return this;
        }

        /**
         * @since 3.0
         */
        public Builder serial(boolean serial) {
            this.serial = serial;
            return this;
        }

        /**
         * @since 3.0
         */
        public Builder dependsOn(String... jobNames) {
            this.dependsOn.addAll(Arrays.asList(jobNames));
            return this;
        }

        /**
         * @since 3.0
         */
        public Builder dependsOn(Collection<String> jobNames) {
            this.dependsOn.addAll(jobNames);
            return this;
        }

        /**
         * Set custom lock name that will be used by a {@link io.bootique.job.lock.LockHandler} implementation to
         * disable this job parallel execution. By default, job name will be used as a lock name.
         *
         * @param lockName optional name of the lock to use
         * @return this builder
         * @since 3.0
         */
        public Builder lockName(String lockName) {
            this.lockName = lockName;
            return this;
        }

        public Builder param(JobParameterMetadata<?> param) {
            this.parameters.add(param);
            return this;
        }

        /**
         * @since 3.0
         */
        public <T> Builder param(String name, String typeName, Function<String, T> parser) {
            return param(name, typeName, parser, null);
        }

        /**
         * @since 3.0
         */
        public <T> Builder param(String name, String typeName, Function<String, T> parser, T defaultValue) {
            this.parameters.add(new JobParameterMetadata<>(name, typeName, parser, defaultValue));
            return this;
        }

        public Builder stringParam(String name) {
            return stringParam(name, null);
        }

        public Builder stringParam(String name, String defaultValue) {
            return param(name, "string", v -> v, defaultValue);
        }

        public Builder dateParam(String name) {
            return dateParam(name, (LocalDate) null);
        }

        public Builder dateParam(String name, String defaultValue) {
            return dateParam(name, parseDate(defaultValue));
        }

        public Builder dateParam(String name, LocalDate defaultValue) {
            return param(name, "date", Builder::parseDate, defaultValue);
        }

        /**
         * @since 3.0
         */
        public Builder dateTimeParam(String name) {
            return dateTimeParam(name, (LocalDateTime) null);
        }

        /**
         * @since 3.0.M1
         */
        public Builder dateTimeParam(String name, String defaultValue) {
            return dateTimeParam(name, parseDateTime(defaultValue));
        }

        /**
         * @since 3.0
         */
        public Builder dateTimeParam(String name, LocalDateTime defaultValue) {
            return param(name, "datetime", Builder::parseDateTime, defaultValue);
        }

        /**
         * @since 3.0
         */
        public Builder intParam(String name) {
            return intParam(name, (Integer) null);
        }

        /**
         * @since 3.0
         */
        public Builder intParam(String name, String defaultValue) {
            return intParam(name, parseInt(defaultValue));
        }

        /**
         * @since 3.0
         */
        public Builder intParam(String name, Integer defaultValue) {
            return param(name, "int", Builder::parseInt, defaultValue);
        }

        public Builder longParam(String name) {
            return longParam(name, (Long) null);
        }

        public Builder longParam(String name, String defaultValue) {
            return longParam(name, parseLong(defaultValue));
        }

        public Builder longParam(String name, Long defaultValue) {
            return param(name, "long", Builder::parseLong, defaultValue);
        }

        public JobMetadata build() {

            if (name == null) {
                throw new IllegalStateException("Job name is not configured");
            }

            if (lockName == null) {
                lockName = name;
            }
            return new JobMetadata(name, parameters, lockName, dependsOn, group, serial);
        }
    }
}
