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
package io.bootique.job.zookeeper.it.job;

import io.bootique.job.BaseJob;
import io.bootique.job.JobMetadata;
import io.bootique.job.SerialJob;
import io.bootique.job.JobResult;
import io.bootique.job.zookeeper.it.ZkJobLockIT;

import java.util.Map;

@SerialJob
public class LockJob extends BaseJob {

    private static final int DELAY = 3_000;

    public LockJob() {
        super(JobMetadata.build(LockJob.class));
    }

    @Override
    public JobResult run(Map<String, Object> params) {
        Integer callsCount = (Integer) params.get(ZkJobLockIT.CALLS_COUNT);
        params.put(ZkJobLockIT.CALLS_COUNT, callsCount + 1);
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            return JobResult.failed();
        }
        return JobResult.succeeded();
    }
}

