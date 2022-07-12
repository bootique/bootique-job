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
import io.bootique.di.Injector;
import io.bootique.job.JobRegistry;
import io.bootique.job.Scheduler;
import io.bootique.job.runtime.GraphExecutor;
import io.bootique.job.runtime.JobDecorators;
import io.bootique.shutdown.ShutdownManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * A configuration object that is used to setup jobs runtime.
 */
@BQConfig("Job scheduler/executor.")
public class SchedulerFactory {

    private Collection<TriggerFactory> triggers;
    private Integer threadPoolSize;
    private Integer graphExecutorThreadPoolSize;

    // TODO: GraphExecutor kinda exists outside of the Scheduler, so probably warrants its own factory
    // TODO: GraphExecutor will become obsolete once project Loom becomes mainstream, and we can use virtual
    //  threads in the main Scheduler pool
    public GraphExecutor createGraphExecutor(Injector injector, ShutdownManager shutdownManager) {
        ExecutorService pool = createGraphExecutorService();
        shutdownManager.addShutdownHook(() -> pool.shutdownNow());
        return new GraphExecutor(pool);
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
        taskScheduler.setPoolSize(createThreadPoolSize());
        taskScheduler.setThreadNamePrefix("bootique-job-");
        taskScheduler.initialize();

        shutdownManager.addShutdownHook(taskScheduler::shutdown);
        return taskScheduler;
    }

    protected int createThreadPoolSize() {
        // TODO: make the default to be equal to the number of CPUs on the system?
        return threadPoolSize != null ? threadPoolSize : 4;
    }

    protected ExecutorService createGraphExecutorService() {
        return Executors.newFixedThreadPool(createGraphExecutorThreadPoolSize(), new GraphExecutorThreadFactory());
    }

    protected int createGraphExecutorThreadPoolSize() {
        return graphExecutorThreadPoolSize != null
                ? graphExecutorThreadPoolSize

                // TODO: that this will compete with the scheduler thread pool, so perhaps there should be a balance
                //   between the two
                : Runtime.getRuntime().availableProcessors();
    }

    @BQConfigProperty("Collection of job triggers.")
    public void setTriggers(Collection<TriggerFactory> triggers) {
        this.triggers = triggers;
    }

    @BQConfigProperty("The max number of worker threads in the scheduler pool. Default is 4")
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    @BQConfigProperty("The max number of worker threads in the scheduler pool. Default is the number of CPU cores on the machine")
    public SchedulerFactory setGraphExecutorThreadPoolSize(Integer graphExecutorThreadPoolSize) {
        this.graphExecutorThreadPoolSize = graphExecutorThreadPoolSize;
        return this;
    }

}
