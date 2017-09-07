package io.bootique.job.runnable;

import java.util.Optional;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * @since 0.24
 */
public class DefaultJobFuture implements JobFuture {

	private String jobName;
	private Optional<RunnableJob> runnable; // TODO: is this needed at all?
	private ScheduledFuture<?> delegate;
	private Supplier<JobResult> resultSupplier;

	public DefaultJobFuture(String jobName,
							RunnableJob runnable,
							ScheduledFuture<?> delegate,
							Supplier<JobResult> resultSupplier) {
		this.jobName = jobName;
		this.runnable = Optional.ofNullable(runnable);
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

	@Override
	public JobResult get() {
		// wait till the job is done and then return the result
		try {
			delegate.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

		return resultSupplier.get();
	}

	@Override
	public JobResult get(long timeout, TimeUnit unit) {

		// wait till the job is done and then return the result
		try {
			delegate.get(timeout, unit);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e);
		}

		return resultSupplier.get();
	}

	@Override
	public String getJobName() {
		return jobName;
	}

}
