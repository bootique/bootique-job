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

package io.bootique.job.scheduler;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.job.JobRegistry;
import io.bootique.job.Scheduler;
import io.bootique.job.runtime.JobDecorators;
import io.bootique.shutdown.ShutdownManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A configuration object that is used to setup jobs runtime.
 */
@BQConfig("Job scheduler/executor.")
public class SchedulerFactory {

    private Collection<TriggerFactory> triggers;
    private int threadPoolSize;

    public SchedulerFactory() {
        this.triggers = new ArrayList<>();
        this.threadPoolSize = 4;
    }

    public Scheduler createScheduler(
            JobRegistry jobRegistry,
            JobDecorators decorators,
            ShutdownManager shutdownManager) {

        TaskScheduler taskScheduler = createTaskScheduler(shutdownManager);

        return new DefaultScheduler(createTriggers(), taskScheduler, jobRegistry, decorators);
    }

    protected Collection<Trigger> createTriggers() {

        if (this.triggers == null) {
            return Collections.emptyList();
        }

        List<Trigger> triggers = new ArrayList<>();
        this.triggers.forEach(t -> triggers.add(t.createTrigger()));
        return triggers;
    }

    protected TaskScheduler createTaskScheduler(ShutdownManager shutdownManager) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(threadPoolSize);
        taskScheduler.setThreadNamePrefix("bootique-job-");
        taskScheduler.initialize();

        shutdownManager.addShutdownHook(taskScheduler::shutdown);
        return taskScheduler;
    }

    @BQConfigProperty("Collection of job triggers.")
    public void setTriggers(Collection<TriggerFactory> triggers) {
        this.triggers = triggers;
    }

    @BQConfigProperty("Minimum number of workers to keep alive (and not allow to time out etc)." +
            " Should be 1 or higher. Default value is 1.")
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
}
