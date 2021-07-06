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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class JobMetadata {

    private final String name;
    private final String lockName;
    private final Collection<JobParameterMetadata<?>> parameters;

    JobMetadata(String name, String lockName, Collection<JobParameterMetadata<?>> parameters) {
        this.name = name;
        this.lockName = lockName;
        this.parameters = parameters;
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

    public static class Builder {

        private String name;
        private String lockName;
        private Collection<JobParameterMetadata<?>> parameters;

        private Builder(String name) {
            this.name = name;
            this.parameters = new ArrayList<>();
        }

        /**
         * Set custom lock name that will be used by a {@link io.bootique.job.lock.LockHandler} implementation to
         * disable this job parallel execution.
         * <br/>
         * By default job name will be used as a lock name.
         *
         * @param lockName optional name of the lock to use
         * @return this builder
         *
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
            if (lockName == null) {
                lockName = name;
            }
            return new JobMetadata(name, lockName, parameters);
        }

    }
}
