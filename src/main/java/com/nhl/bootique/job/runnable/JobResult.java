package com.nhl.bootique.job.runnable;

import com.nhl.bootique.job.JobMetadata;

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
		// TODO: a better result String
		return String.valueOf(outcome);
	}
}
