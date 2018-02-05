package io.bootique.job;

import io.bootique.job.runnable.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @since 0.25
 */
public class JobLogListener implements JobListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLogListener.class);

    @Override
    public void onJobStarted(String jobName, Map<String, Object> parameters, Consumer<Consumer<JobResult>> finishEventSource) {
        LOGGER.info(String.format("job '%s' started with params %s", jobName, parameters));
        finishEventSource.accept(result -> {
            LOGGER.info(String.format("job '%s' finished", jobName));
        });
    }
}
