package io.bootique.job.runnable;

import io.bootique.job.Job;

import java.util.Map;

public class SimpleRunnableJobFactory implements RunnableJobFactory {

    @Override
    public RunnableJob runnable(Job delegate, Map<String, Object> parameters) {
        return () -> delegate.run(parameters);
    }
}
