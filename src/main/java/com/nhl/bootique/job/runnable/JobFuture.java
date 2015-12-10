package com.nhl.bootique.job.runnable;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class JobFuture implements ScheduledFuture<JobResult> {

	private ScheduledFuture<?> delegate;
	private Supplier<JobResult> resultSupplier;

	public JobFuture(ScheduledFuture<?> delegate, Supplier<JobResult> resultSupplier) {
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

}
