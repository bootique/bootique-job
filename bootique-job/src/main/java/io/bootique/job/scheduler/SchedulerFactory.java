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

package io.bootique.job.scheduler;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.job.JobRegistry;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.lock.LockType;
import io.bootique.job.runnable.ErrorHandlingRunnableJobFactory;
import io.bootique.job.runnable.LockAwareRunnableJobFactory;
import io.bootique.job.runnable.RunnableJobFactory;
import io.bootique.job.runnable.SimpleRunnableJobFactory;
import io.bootique.shutdown.ShutdownManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * A configuration object that is used to setup jobs runtime.
 */
@BQConfig("Job scheduler/executor.")
public class SchedulerFactory {

    private Collection<TriggerDescriptor> triggers;
    private int threadPoolSize;
    private boolean clusteredLocks;

    public SchedulerFactory() {
        this.triggers = new ArrayList<>();
        this.threadPoolSize = 4;
    }

    public Scheduler createScheduler(
            Map<LockType, LockHandler> lockHandlers,
            JobRegistry jobRegistry,
            ShutdownManager shutdownManager) {

        for (TriggerDescriptor trigger : triggers) {
            Objects.requireNonNull(trigger, "Job is not specified for trigger: " + trigger.describeTrigger());
        }

        TaskScheduler taskScheduler = createTaskScheduler(shutdownManager);

        LockType lockType = clusteredLocks ? LockType.clustered : LockType.local;
        LockHandler lockHandler = lockHandlers.get(lockType);

        if (lockHandler == null) {
            throw new IllegalStateException("No LockHandler for lock type: " + lockType);
        }

        RunnableJobFactory rf1 = new SimpleRunnableJobFactory();
        RunnableJobFactory rf2 = new LockAwareRunnableJobFactory(rf1, lockHandler, jobRegistry);
        RunnableJobFactory rf3 = new ErrorHandlingRunnableJobFactory(rf2);

        // TODO: do we need to shutdown anything in the scheduler (we already do shutdown of the underlying TaskScheduler)
        return new DefaultScheduler(triggers, taskScheduler, rf3, jobRegistry);
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
    public void setTriggers(Collection<TriggerDescriptor> triggers) {
        this.triggers = triggers;
    }

    @BQConfigProperty("Minimum number of workers to keep alive (and not allow to time out etc)." +
            " Should be 1 or higher. Default value is 1.")
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    @BQConfigProperty("Determines whether the lock handlers will be aware of the Zookeeper cluster.")
    public void setClusteredLocks(boolean clusteredLocks) {
        this.clusteredLocks = clusteredLocks;
    }
}
