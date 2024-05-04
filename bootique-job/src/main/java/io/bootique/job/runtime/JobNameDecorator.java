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
package io.bootique.job.runtime;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobDecorator;
import io.bootique.job.JobOutcome;

import java.util.Map;

/**
 * @since 3.0
 */
public class JobNameDecorator implements JobDecorator {

    @Override
    public boolean isApplicable(JobMetadata metadata, String altName, Map<String, Object> prebindParams) {
        return altName != null && !altName.equals(metadata.getName());
    }

    @Override
    public Job decorate(Job delegate, String altName, Map<String, Object> prebindParams) {
        return isApplicable(delegate.getMetadata(), altName, prebindParams)
                ? new DecoratedJob(delegate, changeName(delegate.getMetadata(), altName), this)
                : delegate;
    }

    protected JobMetadata changeName(JobMetadata metadata, String altName) {
        JobMetadata.Builder builder = JobMetadata.builder(altName);
        metadata.getParameters().forEach(builder::param);
        return builder.build();
    }

    @Override
    public JobOutcome run(Job delegate, Map<String, Object> params) {
        return delegate.run(params);
    }
}
