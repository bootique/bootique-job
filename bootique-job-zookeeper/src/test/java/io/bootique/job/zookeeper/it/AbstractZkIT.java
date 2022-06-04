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
package io.bootique.job.zookeeper.it;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.curator.CuratorModule;
import io.bootique.job.JobModule;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.zookeeper.ZkJobModule;
import io.bootique.job.zookeeper.it.job.LockJob;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@BQTest
public abstract class AbstractZkIT {

    @Container
    static final GenericContainer zk = new GenericContainer("zookeeper:latest").withExposedPorts(2181);

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    protected Scheduler getSchedulerFromRuntime() {
        BQRuntime bqRuntime = testFactory
                .app("--config=classpath:io/bootique/job/zookeeper/it/job-lock.yml")
                .module(b -> BQCoreModule.extend(b).setProperty("bq.curator.connectString", "localhost:" + zk.getMappedPort(2181)))
                .override(JobModule.class).with(ZkJobModule.class)
                .module(new JobModule())
                .module(new CuratorModule())
                .module(b -> JobModule.extend(b).addJob(LockJob.class))
                .createRuntime();
        return bqRuntime.getInstance(Scheduler.class);
    }
}
