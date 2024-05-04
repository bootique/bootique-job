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

package io.bootique.job.fixture;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobOutcome;

import java.util.Map;

public class ParameterizedJob4 extends BaseJob {

    private Map<String, Object> params;

    public ParameterizedJob4() {
        super(JobMetadata.builder(ParameterizedJob4.class)
                .param("xp", "string", s -> "[" + s + "]", "_default_")
                .build());
    }

    @Override
    public JobOutcome run(Map<String, Object> params) {
        this.params = params;
        return JobOutcome.succeeded();
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
