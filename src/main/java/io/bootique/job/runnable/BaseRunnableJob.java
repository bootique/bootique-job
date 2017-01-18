package io.bootique.job.runnable;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseRunnableJob implements RunnableJob {

    private final AtomicInteger running;

    public BaseRunnableJob() {
        this.running = new AtomicInteger();
    }

    @Override
    public JobResult run() {
        running.incrementAndGet();
        try {
            return doRun();
        } finally {
            running.decrementAndGet();
        }
    }

    protected abstract JobResult doRun();

    @Override
    public boolean isRunning() {
        return running.get() > 0;
    }
}
