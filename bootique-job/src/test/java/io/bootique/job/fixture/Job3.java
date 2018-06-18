/**
 *  Licensed to ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.job.fixture;

import io.bootique.job.JobMetadata;

public class Job3 extends ExecutableAtMostOnceJob {

    public Job3() {
        this(0);
    }

    public Job3(long runningTime) {
        this(JobMetadata.build(Job3.class), runningTime, false);
    }

    public Job3(long runningTime, boolean shouldFail) {
        this(JobMetadata.build(Job3.class), runningTime, shouldFail);
    }

    public Job3(JobMetadata metadata) {
        this(metadata, 0, false);
    }

    public Job3(JobMetadata metadata, long runningTime) {
        super(metadata, runningTime, false);
    }

    public Job3(JobMetadata metadata, long runningTime, boolean shouldFail) {
        super(metadata, runningTime, shouldFail);
    }
}
