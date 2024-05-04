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

package io.bootique.job;

/**
 * @since 3.0
 */
public class JobOutcome {

    private final JobStatus status;
    private final Throwable exception;
    private final String message;
    private final JobFuture yieldedTo;

    public static JobOutcome succeeded() {
        return new JobOutcome(JobStatus.SUCCESS, null, null, null);
    }

    public static JobOutcome succeeded(String message) {
        return new JobOutcome(JobStatus.SUCCESS, null, message, null);
    }

    public static JobOutcome failed() {
        return new JobOutcome(JobStatus.FAILURE, null, null, null);
    }

    public static JobOutcome failed(String message) {
        return new JobOutcome(JobStatus.FAILURE, null, message, null);
    }

    public static JobOutcome failed(Throwable th) {
        return new JobOutcome(JobStatus.FAILURE, th, null, null);
    }

    public static JobOutcome failed(String message, Throwable th) {
        return new JobOutcome(JobStatus.FAILURE, th, message, null);
    }

    public static JobOutcome succeededPartially() {
        return new JobOutcome(JobStatus.PARTIAL_SUCCESS, null, null, null);
    }

    public static JobOutcome succeededPartially(String message) {
        return new JobOutcome(JobStatus.PARTIAL_SUCCESS, null, message, null);
    }

    public static JobOutcome unknown() {
        return new JobOutcome(JobStatus.UNKNOWN, null, null, null);
    }

    public static JobOutcome unknown(Throwable th) {
        return new JobOutcome(JobStatus.UNKNOWN, th, null, null);
    }

    public static JobOutcome unknown(String message) {
        return new JobOutcome(JobStatus.UNKNOWN, null, message, null);
    }

    public static JobOutcome skipped() {
        return new JobOutcome(JobStatus.SKIPPED, null, null, null);
    }

    public static JobOutcome skipped(String message) {
        return new JobOutcome(JobStatus.SKIPPED, null, message, null);
    }

    protected JobOutcome(JobStatus status, Throwable exception, String message, JobFuture yieldedTo) {
        this.status = status;
        this.exception = exception;
        this.message = message;
        this.yieldedTo = yieldedTo;
    }

    /**
     * @deprecated use {@link #getStatus()}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public JobStatus getOutcome() {
        return status;
    }

    public JobStatus getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == JobStatus.SUCCESS;
    }

    /**
     * @deprecated use {@link #getException()}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public Throwable getThrowable() {
        return exception;
    }

    public Throwable getException() {
        return exception;
    }

    public String getMessage() {
        return message;
    }

    public JobFuture getYieldedTo() {
        return yieldedTo;
    }

    @Override
    public String toString() {

        String message = this.message;

        if (message == null && exception != null) {
            message = exception.getMessage();
        }

        StringBuilder buffer = new StringBuilder().append("[").append(status);
        if (message != null) {
            buffer.append(": ").append(message);
        }

        return buffer.append("]").toString();
    }
}
