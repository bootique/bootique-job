package io.bootique.job.consul.it;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.bootique.BQRuntime;
import io.bootique.job.consul.ConsulJobModule;
import io.bootique.job.consul.it.job.LockJob;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.test.junit.BQTestFactory;
import org.junit.ClassRule;
import org.junit.Rule;
import org.testcontainers.containers.GenericContainer;

import java.util.function.Consumer;

public abstract class AbstractConsulTest {

    private static final int HOST_PORT = 8500;
    private static final int CONTAINER_EXPOSED_PORT = 8500;
    private static final Consumer<CreateContainerCmd> MAPPING_CMD =
            e -> e.withPortBindings(
                    new PortBinding(Ports.Binding.bindPort(HOST_PORT),
                            new ExposedPort(CONTAINER_EXPOSED_PORT))
            );

    @ClassRule
    public static GenericContainer consul = new GenericContainer("consul:latest")
                                            .withCreateContainerCmdModifier(MAPPING_CMD)
                                            .withExposedPorts(CONTAINER_EXPOSED_PORT);

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    protected Scheduler getSchedulerFromRuntime(String yamlConfigPath) {
        BQRuntime bqRuntime = testFactory
                .app(yamlConfigPath)
                .override(JobModule.class).with(ConsulJobModule.class)
                .module(new JobModule())
                .module(b -> JobModule.extend(b).addJob(LockJob.class))
                .createRuntime();
        return bqRuntime.getInstance(Scheduler.class);
    }

}
