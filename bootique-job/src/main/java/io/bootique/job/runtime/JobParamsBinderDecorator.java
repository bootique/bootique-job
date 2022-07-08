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
import io.bootique.job.JobResult;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.0
 */
public class JobParamsBinderDecorator implements JobDecorator {

    @Override
    public boolean isApplicable(JobMetadata metadata, String altName, Map<String, Object> prebindParams) {
        return !prebindParams.isEmpty();
    }

    @Override
    public Job decorate(Job delegate, String altName, Map<String, Object> prebindParams) {
        return isApplicable(delegate.getMetadata(), altName, prebindParams)
                ? new KnownParamsDecorator(prebindParams).decorate(delegate, altName, prebindParams)
                : delegate;
    }

    @Override
    public JobResult run(Job delegate, Map<String, Object> params) {
        throw new UnsupportedOperationException("This decorator is not executable. It delegates to another decorator instead");
    }

    static class KnownParamsDecorator implements JobDecorator {
        private final Map<String, Object> prebindParams;

        KnownParamsDecorator(Map<String, Object> prebindParams) {
            this.prebindParams = prebindParams;
        }

        @Override
        public JobResult run(Job delegate, Map<String, Object> params) {
            return delegate.run(mergeParams(params));
        }

        protected Map<String, Object> mergeParams(Map<String, Object> overridingParams) {
            Map<String, Object> merged = new HashMap<>(prebindParams);
            merged.putAll(overridingParams);
            return merged;
        }
    }
}
