package io.bootique.job.scheduler;

import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

@Deprecated
public class TriggerDescriptorDeprecatedTest {
    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testDeprecatedSupport_FixedDelay(){
        TriggerDescriptor triggerDescriptor = new TriggerDescriptor();
        assertNull(triggerDescriptor.getFixedDelay());
        triggerDescriptor.setFixedDelayMs(4000);
        assertNotNull(triggerDescriptor.getFixedDelay());
        assertEquals(4000, triggerDescriptor.getFixedDelay().getDuration().toMillis());
    }

    @Test
    public void testDeprecatedSupport_FixedRate(){
        TriggerDescriptor triggerDescriptor = new TriggerDescriptor();
        assertNull(triggerDescriptor.getFixedRate());
        triggerDescriptor.setFixedRateMs(4000);
        assertNotNull(triggerDescriptor.getFixedRate());
        assertEquals(4000, triggerDescriptor.getFixedRate().getDuration().toMillis());
    }

    @Test
    public void testDeprecatedSupport_InitialDelay(){
        TriggerDescriptor triggerDescriptor = new TriggerDescriptor();
        assertNotNull(triggerDescriptor.getInitialDelay());
        triggerDescriptor.setInitialDelayMs(4000);
        assertNotNull(triggerDescriptor.getInitialDelay());
        assertEquals(4000, triggerDescriptor.getInitialDelay().getDuration().toMillis());
    }

}
