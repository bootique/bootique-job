package com.nhl.springboot.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.nhl.launcher.job.JobMetadata;
import com.nhl.launcher.job.JobMetadataBuilder;
import com.nhl.launcher.job.JobParameterMetadata;

public class JobMetadataBuilderTest {

	@Test
	public void testBuild_static() {

		JobMetadata j = JobMetadataBuilder.build("nn");
		assertNotNull(j);
		assertEquals("nn", j.getName());
		assertTrue(j.getParameters().isEmpty());
	}

	@Test
	public void testBuild_Name() {

		JobMetadata j = JobMetadataBuilder.newBuilder("nn").build();
		assertNotNull(j);
		assertEquals("nn", j.getName());
		assertTrue(j.getParameters().isEmpty());
	}

	@Test
	public void testBuild_Params() {

		JobMetadata j = JobMetadataBuilder.newBuilder("nn").dateParam("dd", "2015-02-04").stringParam("ss", "ssv")
				.longParam("ll", "34556775").build();
		assertEquals(3, j.getParameters().size());

		List<JobParameterMetadata<?>> params = new ArrayList<>(j.getParameters());
		assertEquals("dd", params.get(0).getName());
		assertEquals(LocalDate.of(2015, 2, 4), params.get(0).fromString(null));

		assertEquals("ss", params.get(1).getName());
		assertEquals("ssv", params.get(1).fromString(null));

		assertEquals("ll", params.get(2).getName());
		assertEquals(new Long(34556775), params.get(2).fromString(null));

	}
}
