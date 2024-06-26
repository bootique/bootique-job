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
package io.bootique.job.consul.lock;

import io.bootique.job.Job;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.JobOutcome;

import java.util.Map;

public class CompositeConsulLockHandler implements LockHandler {

    private final LockHandler localLockHandler;
    private final LockHandler consulLockHandler;

    public CompositeConsulLockHandler(LockHandler localLockHandler, LockHandler consulLockHandler) {
        this.localLockHandler = localLockHandler;
        this.consulLockHandler = consulLockHandler;
    }

    @Override
    public JobOutcome run(Job delegate, Map<String, Object> params) {
        return localLockHandler.run(consulLockHandler.decorate(delegate, null, params), params);
    }
}
