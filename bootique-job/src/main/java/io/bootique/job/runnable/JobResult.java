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

import io.bootique.job.JobMetadata;

public class JobResult {

	private JobMetadata metadata;
	private JobOutcome outcome;
	private Throwable throwable;
	private String message;

	public static JobResult success(JobMetadata metadata) {
		return new JobResult(metadata, JobOutcome.SUCCESS, null, null);
	}

	public static JobResult failure(JobMetadata metadata) {
		return new JobResult(metadata, JobOutcome.FAILURE, null, null);
	}
	
	public static JobResult failure(JobMetadata metadata, String message) {
		return new JobResult(metadata, JobOutcome.FAILURE, null, message);
	}

	public static JobResult failure(JobMetadata metadata, Throwable th) {
		return new JobResult(metadata, JobOutcome.FAILURE, th, null);
	}

	/**
	 * @since 3.0
	 */
	public static JobResult partialSuccess(JobMetadata metadata) {
		return new JobResult(metadata, JobOutcome.PARTIAL_SUCCESS, null, null);
	}

	/**
	 * @since 3.0
	 */
	public static JobResult yielded(JobMetadata metadata) {
		return new JobResult(metadata, JobOutcome.YIELDED, null, null);
	}

	public static JobResult unknown(JobMetadata metadata) {
		return new JobResult(metadata, JobOutcome.UNKNOWN, null, null);
	}

	public static JobResult unknown(JobMetadata metadata, Throwable th) {
		return new JobResult(metadata, JobOutcome.UNKNOWN, th, null);
	}

	public JobResult(JobMetadata metadata, JobOutcome outcome, Throwable throwable, String message) {
		this.metadata = metadata;
		this.outcome = outcome;
		this.throwable = throwable;
		this.message = message;
	}

	public JobMetadata getMetadata() {
		return metadata;
	}

	public JobOutcome getOutcome() {
		return outcome;
	}

	public boolean isSuccess() {
		return outcome == JobOutcome.SUCCESS;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {

		String message = this.message;

		if (message == null && throwable != null) {
			message = throwable.getMessage();
		}

		StringBuilder buffer = new StringBuilder().append("[").append(outcome);
		if (message != null) {
			buffer.append(": ").append(message);
		}

		return buffer.append("]").toString();
	}
}
