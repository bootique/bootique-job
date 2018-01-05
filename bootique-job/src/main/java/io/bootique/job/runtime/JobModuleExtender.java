package io.bootique.job.runtime;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.bootique.job.Job;
import io.bootique.job.JobListener;
import io.bootique.job.MappedJobListener;

/**
 * @since 0.14
 */
public class JobModuleExtender {

    private Binder binder;
    private Multibinder<Job> jobs;
    private Multibinder<JobListener> listeners;
    private Multibinder<MappedJobListener> mappedListeners;

    JobModuleExtender(Binder binder) {
        this.binder = binder;
    }

    JobModuleExtender initAllExtensions() {
        contributeListeners();
        contributeMappedListeners();
        contributeJobs();

        return this;
    }

    public JobModuleExtender addJob(Job job) {
        contributeJobs().addBinding().toInstance(job);
        return this;
    }

    public JobModuleExtender addJob(Class<? extends Job> jobType) {
        // TODO: what does singleton scope means when adding to collection?
        contributeJobs().addBinding().to(jobType).in(Singleton.class);
        return this;
    }

    public JobModuleExtender addListener(JobListener listener) {
        contributeListeners().addBinding().toInstance(listener);
        return this;
    }

    /**
     * Adds a listener to the set of Job listeners.
     *
     * @param mappedJobListener a wrapped listener
     * @param <T>
     * @return this extender instance
     */
    public <T extends JobListener> JobModuleExtender addMappedListener(MappedJobListener<T> mappedJobListener) {
        contributeMappedListeners().addBinding().toInstance(mappedJobListener);
        return this;
    }

    /**
     * Adds a listener of the specified type to the set of Job listeners.
     *
     * @param mappedJobListenerKey binding key
     * @param <T>
     * @return this extender instance
     */
    public <T extends JobListener> JobModuleExtender addMappedListener(Key<MappedJobListener<T>> mappedJobListenerKey) {
        contributeMappedListeners().addBinding().to(mappedJobListenerKey);
        return this;
    }

    /**
     * Adds a listener of the specified type to the set of Job listeners.
     *
     * @param mappedJobListenerType listener type
     * @param <T>
     * @return this extender instance
     */
    public <T extends JobListener> JobModuleExtender addMappedListener(TypeLiteral<MappedJobListener<T>> mappedJobListenerType) {
        contributeMappedListeners().addBinding().to(mappedJobListenerType);
        return this;
    }

    public JobModuleExtender addListener(Class<? extends JobListener> listenerType) {
        // TODO: what does singleton scope means when adding to collection?
        contributeListeners().addBinding().to(listenerType).in(Singleton.class);
        return this;
    }

    protected Multibinder<Job> contributeJobs() {
        if (jobs == null) {
            jobs = Multibinder.newSetBinder(binder, Job.class);
        }

        return jobs;
    }

    protected Multibinder<JobListener> contributeListeners() {
        if (listeners == null) {
            listeners = Multibinder.newSetBinder(binder, JobListener.class);
        }
        return listeners;
    }

    protected Multibinder<MappedJobListener> contributeMappedListeners() {
        if (mappedListeners == null) {
            mappedListeners = Multibinder.newSetBinder(binder, MappedJobListener.class);
        }
        return mappedListeners;
    }

}
