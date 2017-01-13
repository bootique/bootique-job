package io.bootique.job.runnable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class JobFuture implements ScheduledFuture<JobResult> {

	/**
	 * @since 0.13
     */
	public static Builder forJob(String job) {
		return new Builder(job);
	}

	private String job;
	private RunnableJob runnable;
	private ScheduledFuture<?> delegate;
	private Supplier<JobResult> resultSupplier;

	public JobFuture(String job,
					 RunnableJob runnable,
					 ScheduledFuture<?> delegate,
					 Supplier<JobResult> resultSupplier) {
		this.job = job;
		this.runnable = runnable;
		this.delegate = delegate;
		this.resultSupplier = resultSupplier;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return delegate.getDelay(unit);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return delegate.cancel(mayInterruptIfRunning);
	}

	@Override
	public int compareTo(Delayed o) {
		return delegate.compareTo(o);
	}

	@Override
	public boolean isCancelled() {
		return delegate.isCancelled();
	}

	@Override
	public boolean isDone() {
		return delegate.isDone();
	}

	/**
	 * @return Job name
	 * @since 0.13
     */
	public String getJob() {
		return job;
	}

	/**
	 * @return Runnable job implementation
	 * @since 0.13
     */
	public RunnableJob getRunnable() {
		return runnable;
	}

	public JobResult get() {
		// wait till the job is done and then return the result
		try {
			delegate.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

		return resultSupplier.get();
	}

	public JobResult get(long timeout, TimeUnit unit) {

		// wait till the job is done and then return the result
		try {
			delegate.get(timeout, unit);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e);
		}

		return resultSupplier.get();
	}

	public static class Builder {

		private String job;
		private RunnableJob runnable;
		private ScheduledFuture<?> future;
		private Supplier<JobResult> resultSupplier;

		public Builder(String job) {
			this.job = Objects.requireNonNull(job);
		}

		public Builder runnable(RunnableJob runnable) {
			this.runnable = runnable;
			return this;
		}

		public Builder future(ScheduledFuture<?> future) {
			this.future = future;
			return this;
		}

		public Builder resultSupplier(Supplier<JobResult> resultSupplier) {
			this.resultSupplier = resultSupplier;
			return this;
		}

		public JobFuture build() {
			Objects.requireNonNull(runnable);
			Objects.requireNonNull(future);
			Objects.requireNonNull(resultSupplier);
			return new JobFuture(job, runnable, future, resultSupplier);
		}
	}
}
