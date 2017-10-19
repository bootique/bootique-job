package io.bootique.job;

public class Utils {

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpectedly interrupted", e);
        }
    }
}
