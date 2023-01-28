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
package io.bootique.job.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bootique.job.JobRegistry;

import java.util.Map;
import java.util.Objects;

/**
 * @since 3.0
 */
public class JobExecParser {

    private static final char JOB_PARAMS_SEPARATOR = '{';

    private final JobRegistry jobRegistry;
    private final ObjectMapper paramsParser;

    public JobExecParser(JobRegistry jobRegistry, ObjectMapper paramsParser) {
        this.jobRegistry = jobRegistry;
        this.paramsParser = paramsParser;
    }

    public JobExec parse(String jobWithParams) {

        Objects.requireNonNull(jobWithParams);
        if (jobWithParams.length() == 0) {
            throw new IllegalArgumentException("Empty job name");
        }

        int paramsIndex = jobWithParams.indexOf(JOB_PARAMS_SEPARATOR);
        if (paramsIndex < 0) {
            // use a mutable
            return new JobExec(jobWithParams);
        } else if (paramsIndex == 0) {
            throw new IllegalArgumentException("Job name can't start with '" + JOB_PARAMS_SEPARATOR + "': " + jobWithParams);
        } else if (paramsIndex == jobWithParams.length() - 1) {
            return new JobExec(jobWithParams.substring(0, paramsIndex));
        } else {
            String jobName = jobWithParams.substring(0, paramsIndex);
            return new JobExec(
                    jobName,
                    parseParams(jobName, jobWithParams.substring(paramsIndex)));
        }
    }

    private Map<String, Object> parseParams(String jobName, String paramsString) {

        Map<String, Object> params;
        try {
            params = paramsParser.readValue(paramsString, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Not a valid JSON map: " + paramsString, e);
        }

        return params.isEmpty()
                ? params
                : jobRegistry.getJob(jobName).getMetadata().convertParameters(params);
    }
}
