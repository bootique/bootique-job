package io.bootique.job.scheduler;

import io.bootique.BQRuntime;
import io.bootique.config.ConfigurationFactory;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.runtime.JobModuleProvider;
import io.bootique.test.junit.BQTestFactory;
import io.bootique.test.junit.BQTestRuntimeBuilder;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class TriggerDescriptorTest {
    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testDeprecatedSupport_FixedDelay(){
        TriggerDescriptor triggerDescriptor = new TriggerDescriptor();
        assertNull(triggerDescriptor.getFixedDelay());
        triggerDescriptor.setFixedDelayMs(4000);
        assertNotNull(triggerDescriptor.getFixedDelay());
        assertNotNull(triggerDescriptor.getFixedDelay().getDuration());

        assertEquals(triggerDescriptor.getFixedDelayMs(), triggerDescriptor.getFixedDelay().getDuration().toMillis());
        assertEquals(4000, triggerDescriptor.getFixedDelay().getDuration().toMillis());

    }

    @Test
    public void testDeprecatedSupport_FixedRate(){
        TriggerDescriptor triggerDescriptor = new TriggerDescriptor();
        assertNull(triggerDescriptor.getFixedRate());
        triggerDescriptor.setFixedRateMs(4000);
        assertNotNull(triggerDescriptor.getFixedRate());
        assertNotNull(triggerDescriptor.getFixedRate().getDuration());

        assertEquals(triggerDescriptor.getFixedRateMs(), triggerDescriptor.getFixedRate().getDuration().toMillis());
        assertEquals(4000, triggerDescriptor.getFixedRate().getDuration().toMillis());
    }

    @Test
    public void testDeprecatedSupport_InitialDelay(){
        TriggerDescriptor triggerDescriptor = new TriggerDescriptor();
        assertNotNull(triggerDescriptor.getInitialDelay());
        triggerDescriptor.setInitialDelayMs(4000);
        assertNotNull(triggerDescriptor.getInitialDelay());
        assertNotNull(triggerDescriptor.getInitialDelay().getDuration());

        assertEquals(triggerDescriptor.getInitialDelayMs(), triggerDescriptor.getInitialDelay().getDuration().toMillis());
        assertEquals(4000, triggerDescriptor.getInitialDelay().getDuration().toMillis());
    }

//    @Test
//    public void mappingTest() {
//        BQRuntime runtime = testFactory
//                .app("--config=classpath:io/bootique/job/cron.yml")
//                .module(new JobModuleProvider())
//                .createRuntime();
//
//        TriggerDescriptor instance = runtime.getInstance(TriggerDescriptor.class);
//
//    }

}
