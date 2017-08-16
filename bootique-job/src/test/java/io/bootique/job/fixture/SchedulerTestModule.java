package io.bootique.job.fixture;

import com.google.inject.Binder;
import com.google.inject.Module;
import io.bootique.job.JobListener;
import io.bootique.job.runtime.JobModule;

import java.util.Collection;

public class SchedulerTestModule implements Module {

    private final Collection<JobListener> listeners;

    public SchedulerTestModule(Collection<JobListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void configure(Binder binder) {
        JobModule.extend(binder).addJob(ScheduledJob1.class);
        for (JobListener listener : listeners) {
            JobModule.extend(binder).addListener(listener);
        }
    }
}
