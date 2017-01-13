package io.bootique.job.runnable;

import java.util.Map;

public interface RunnableJob {

	JobResult run();

	/**
	 * @return Parameters that the job was submitted with.
	 * @since 0.13
     */
	Map<String, Object> getParameters();

	/**
	 * @return true if the job is currently executing its' {@link #run()} method.
	 * @since 0.13
     */
	boolean isRunning();
}
