package io.bootique.job.runnable;

import java.util.Map;

public interface RunnableJob {

	JobResult run();

	Map<String, Object> getParameters();

	boolean isRunning();
}
