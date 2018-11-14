package io.bootique.job.value;

import org.junit.Test;

import static org.junit.Assert.*;

public class CronTest {

    @Test
    public void expressionTest() {
        String expression = "* * 8 0 * *";
        Cron cron = new Cron(expression);
        assertEquals( "Cron {" +
                "expression='" + expression + '\'' +
                '}', cron.toString());

    }

    @Test
    public void expressionEquals() {
        String expression1 = "* * 8 0 * *";
        String expression2 = "* * 8 * 0 *";
        String expression3 = "* * 8 0 * *";

        Cron cron1 = new Cron(expression1);
        Cron cron2 = new Cron(expression2);
        Cron cron3 = new Cron(expression3);
        Cron cron4 = new Cron(null);

        assertTrue(cron1.equals(cron3));
        assertFalse(cron1.equals(null));
        assertFalse(cron4.equals(null));
        assertFalse(cron1.equals(cron2));
        assertTrue(cron1.equals(cron3));
    }

    @Test
    public void expressionHashcode() {
        String expression1 = "* * 8 0 * *";
        String expression2 = "* * 8 * 0 *";
        String expression3 = "* * 8 0 * *";

        Cron cron1 = new Cron(expression1);
        Cron cron2 = new Cron(expression2);
        Cron cron3 = new Cron(expression3);
        Cron cron4 = new Cron(null);
        Cron cron5 = new Cron("0");

        assertEquals(cron1.hashCode(), cron3.hashCode());
        assertNotEquals(cron1.hashCode(), cron2.hashCode());
        assertNotEquals(cron1.hashCode(), cron2.hashCode());
        assertEquals(0, cron4.hashCode());
        assertNotEquals(0, cron5.hashCode());
    }

}
