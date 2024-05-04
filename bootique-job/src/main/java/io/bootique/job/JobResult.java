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
 * @deprecated in favor of {@link JobOutcome}
 */
@Deprecated(since = "3.0", forRemoval = true)
public class JobResult extends JobOutcome {

    private final JobMetadata metadata;

    /**
     * @deprecated in favor of {@link #succeeded()}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult success(JobMetadata metadata) {
        return new JobResult(metadata, JobStatus.SUCCESS, null, null, null);
    }

    /**
     * @since 3.0
     * @deprecated in favor of {@link #succeeded(String)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult success(JobMetadata metadata, String message) {
        return new JobResult(metadata, JobStatus.SUCCESS, null, message, null);
    }

    /**
     * @deprecated in favor of {@link #failed()}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult failure(JobMetadata metadata) {
        return new JobResult(metadata, JobStatus.FAILURE, null, null, null);
    }

    /**
     * @deprecated in favor of {@link #failed(String)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult failure(JobMetadata metadata, String message) {
        return new JobResult(metadata, JobStatus.FAILURE, null, message, null);
    }

    /**
     * @deprecated in favor of {@link #failed(Throwable)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult failure(JobMetadata metadata, Throwable th) {
        return new JobResult(metadata, JobStatus.FAILURE, th, null, null);
    }

    /**
     * @deprecated in favor of {@link #failed(String, Throwable)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult failure(JobMetadata metadata, String message, Throwable th) {
        return new JobResult(metadata, JobStatus.FAILURE, th, message, null);
    }

    /**
     * @deprecated in favor of {@link #succeededPartially()}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult partialSuccess(JobMetadata metadata) {
        return new JobResult(metadata, JobStatus.PARTIAL_SUCCESS, null, null, null);
    }

    /**
     * @deprecated in favor of {@link #succeededPartially(String)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult partialSuccess(JobMetadata metadata, String message) {
        return new JobResult(metadata, JobStatus.PARTIAL_SUCCESS, null, message, null);
    }

    /**
     * @deprecated in favor of {@link #unknown()}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult unknown(JobMetadata metadata) {
        return new JobResult(metadata, JobStatus.UNKNOWN, null, null, null);
    }


    /**
     * @deprecated in favor of {@link #unknown(Throwable)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult unknown(JobMetadata metadata, Throwable th) {
        return new JobResult(metadata, JobStatus.UNKNOWN, th, null, null);
    }
    /**
     * @deprecated in favor of {@link #unknown(String)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult unknown(JobMetadata metadata, String message) {
        return new JobResult(metadata, JobStatus.UNKNOWN, null, message, null);
    }

    /**
     * @deprecated in favor of {@link #skipped()}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult skipped(JobMetadata metadata) {
        return new JobResult(metadata, JobStatus.SKIPPED, null, null, null);
    }

    /**
     * @deprecated in favor of {@link #skipped(String)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobResult skipped(JobMetadata metadata, String message) {
        return new JobResult(metadata, JobStatus.SKIPPED, null, message, null);
    }

    /**
     * @deprecated make possible job lambdas. Current callers must use {@link Job#getMetadata()} instead.
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public JobMetadata getMetadata() {
        return metadata;
    }

    protected JobResult(JobMetadata metadata, JobStatus status, Throwable throwable, String message, JobFuture yieldedTo) {
        super(status, throwable, message, yieldedTo);
        this.metadata = metadata;
    }
}
