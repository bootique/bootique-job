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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class JobMetadata {

    private final String name;
    private final Collection<JobParameterMetadata<?>> parameters;
    private final boolean serial;

    JobMetadata(String name, Collection<JobParameterMetadata<?>> parameters, boolean serial) {
        this.name = name;
        this.parameters = parameters;
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

    public Collection<JobParameterMetadata<?>> getParameters() {
        return parameters != null ? parameters : Collections.emptyList();
    }

    /**
     * @since 3.0
     */
    public boolean isSerial() {
        return serial;
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
        private boolean serial;

        private Builder(String name) {
            this.name = name;
            this.parameters = new ArrayList<>();
        }

        /**
         * @since 3.0
         */
        public Builder serial(boolean serial) {
            this.serial = serial;
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
         * @since 3.0.M1
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
         * @since 3.0.M1
         */
        public Builder dateTimeParam(String name, LocalDateTime defaultValue) {
            return param(name, "datetime", Builder::parseDateTime, defaultValue);
        }

        /**
         * @since 3.0.M1
         */
        public Builder intParam(String name) {
            return intParam(name, (Integer) null);
        }

        /**
         * @since 3.0.M1
         */
        public Builder intParam(String name, String defaultValue) {
            return intParam(name, parseInt(defaultValue));
        }

        /**
         * @since 3.0.M1
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

            return new JobMetadata(name, parameters, serial);
        }
    }
}
