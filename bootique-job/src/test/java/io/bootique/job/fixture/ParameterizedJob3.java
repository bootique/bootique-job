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
import io.bootique.job.runnable.JobResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParameterizedJob3 extends BaseJob {

	public ParameterizedJob3() {
		super(JobMetadata.builder(ParameterizedJob3.class).longParam("longp", 333L).build());
	}

	@Override
	public JobResult run(Map<String, Object> params) {
		assertEquals(3, params.size());
		assertTrue(params.containsKey("longp"));
		assertTrue(params.containsKey("param1"));
		assertTrue(params.containsKey("param2"));
		assertEquals(3L, params.get("longp"));
		assertEquals("value1", params.get("param1"));
		assertEquals(2, params.get("param2"));
		return JobResult.success(getMetadata());
	}
}
