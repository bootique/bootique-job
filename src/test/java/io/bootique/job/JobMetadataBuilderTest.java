package io.bootique.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import io.bootique.job.JobMetadata;
import io.bootique.job.JobParameterMetadata;
import org.junit.Test;

public class JobMetadataBuilderTest {

	@Test
	public void testBuild_static() {

		JobMetadata j = JobMetadata.build("nn");
		assertNotNull(j);
		assertEquals("nn", j.getName());
		assertTrue(j.getParameters().isEmpty());
	}

	@Test
	public void testBuild_Name() {

		JobMetadata j = JobMetadata.builder("nn").build();
		assertNotNull(j);
		assertEquals("nn", j.getName());
		assertTrue(j.getParameters().isEmpty());
	}

	@Test
	public void testBuild_Params() {

		JobMetadata j = JobMetadata.builder("nn").dateParam("dd", "2015-02-04").stringParam("ss", "ssv")
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
