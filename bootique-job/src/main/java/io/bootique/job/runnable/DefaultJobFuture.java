/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
