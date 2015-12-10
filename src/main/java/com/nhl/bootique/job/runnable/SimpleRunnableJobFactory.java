package com.nhl.bootique.job.runnable;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.bootique.job.Job;

public class SimpleRunnableJobFactory implements RunnableJobFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRunnableJobFactory.class);

	@Override
	public RunnableJob runnable(Job job, Map<String, Object> parameters) {
		return () -> {

			LOGGER.info(String.format("job '%s' started with params %s", job.getMetadata().getName(), parameters));

			try {
				return job.run(parameters);
			} finally {
				LOGGER.info(String.format("job '%s' finished", job.getMetadata().getName()));
			}
		};
	}
}
