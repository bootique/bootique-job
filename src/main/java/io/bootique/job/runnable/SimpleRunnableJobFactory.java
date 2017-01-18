package io.bootique.job.runnable;

import java.util.Map;

import io.bootique.job.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRunnableJobFactory implements RunnableJobFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRunnableJobFactory.class);

	@Override
	public RunnableJob runnable(Job job, Map<String, Object> parameters) {
		return new BaseRunnableJob() {
			@Override
			protected JobResult doRun() {
				LOGGER.info(String.format("job '%s' started with params %s", job.getMetadata().getName(), parameters));

				try {
					return job.run(parameters);
				} finally {
					LOGGER.info(String.format("job '%s' finished", job.getMetadata().getName()));
				}
			}

			@Override
			public Map<String, Object> getParameters() {
				return parameters;
			}
		};
	}
}
