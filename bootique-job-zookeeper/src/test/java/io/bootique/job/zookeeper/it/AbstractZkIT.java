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

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.bootique.BQRuntime;
import io.bootique.curator.CuratorModule;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.zookeeper.ZkJobModule;
import io.bootique.job.zookeeper.it.job.LockJob;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.function.Consumer;

@Testcontainers
@BQTest
public abstract class AbstractZkIT {

    private static final int HOST_PORT_1 = 2181;
    private static final int CONTAINER_EXPOSED_PORT_1 = 2181;
    private static final Consumer<CreateContainerCmd> MAPPING_CMD =
            e -> e.withPortBindings(
                    new PortBinding(
                            Ports.Binding.bindPort(HOST_PORT_1),
                            new ExposedPort(CONTAINER_EXPOSED_PORT_1)
                    )
            );

    @Container
    public static GenericContainer zookeeper = new GenericContainer("zookeeper:latest")
            .withCreateContainerCmdModifier(MAPPING_CMD)
            .withExposedPorts(CONTAINER_EXPOSED_PORT_1);

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    protected Scheduler getSchedulerFromRuntime(String yamlConfigPath) {
        BQRuntime bqRuntime = testFactory
                .app(yamlConfigPath)
                .override(JobModule.class).with(ZkJobModule.class)
                .module(new JobModule())
                .module(new CuratorModule())
                .module(b -> JobModule.extend(b).addJob(LockJob.class))
                .createRuntime();
        return bqRuntime.getInstance(Scheduler.class);
    }
}
