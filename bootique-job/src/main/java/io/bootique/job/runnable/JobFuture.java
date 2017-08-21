package io.bootique.job.runnable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface JobFuture extends ScheduledFuture<JobResult> {

    /**
     * @return Job name
     * @since 0.24
     */
    String getJobName();

    /**
	 * @since 0.13
     */
	static JobFutureBuilder forJob(String job) {
		return new JobFutureBuilder(job);
	}

	/**
	 * Wait till the job is done and then return the result
	 *
	 * @since 0.13
     */
	JobResult get();

	/**
	 * Wait till the job is done and then return the result.
	 * Throws an exception, if time elapses before the job has finished.
	 *
	 * @since 0.13
     */
	JobResult get(long timeout, TimeUnit unit);
}
