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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class JobMetadataBuilderTest {

	@Test
	public void testBuild_static() {

		JobMetadata j = JobMetadata.build("nn");
		assertNotNull(j);
		assertEquals("nn", j.getName());
		assertEquals("nn", j.getLockName());
		assertTrue(j.getParameters().isEmpty());
	}

	@Test
	public void testBuild_Name() {

		JobMetadata j = JobMetadata.builder("nn").build();
		assertNotNull(j);
		assertEquals("nn", j.getName());
		assertEquals("nn", j.getLockName());
		assertTrue(j.getParameters().isEmpty());
	}

	@Test
	public void testBuild_LockName() {

		JobMetadata j = JobMetadata.builder("nn").lockName("ll").build();
		assertNotNull(j);
		assertEquals("nn", j.getName());
		assertEquals("ll", j.getLockName());
		assertTrue(j.getParameters().isEmpty());
	}

	@Test
	public void testBuild_Params() {

		JobMetadata j = JobMetadata.builder("nn")
				.dateParam("dd", "2015-02-04")
				.stringParam("ss", "ssv")
				.longParam("ll", "34556775")
				.build();
		assertEquals(3, j.getParameters().size());

		List<JobParameterMetadata<?>> params = new ArrayList<>(j.getParameters());
		assertEquals("dd", params.get(0).getName());
		assertEquals(LocalDate.of(2015, 2, 4), params.get(0).fromString(null));

		assertEquals("ss", params.get(1).getName());
		assertEquals("ssv", params.get(1).fromString(null));

		assertEquals("ll", params.get(2).getName());
		assertEquals(34556775L, params.get(2).fromString(null));

	}
}
