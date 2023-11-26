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

import io.bootique.di.Binder;

/**
 * @deprecated split into two modules {@link JobsModule} and {@link SchedulerModule} with their own configuration
 * prefixes instead of managing both prefixes in one module.
 */
@Deprecated(since = "3.0", forRemoval = true)
public class JobModule {

    public static final String JOB_OPTION = SchedulerModule.JOB_OPTION;

    /**
     * @deprecated use {@link JobsModule#extend(Binder)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static JobsModuleExtender extend(Binder binder) {
        return JobsModule.extend(binder);
    }
}
