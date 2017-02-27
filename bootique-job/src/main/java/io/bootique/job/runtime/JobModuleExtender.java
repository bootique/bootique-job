package io.bootique.job.runtime;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.bootique.job.Job;
import io.bootique.job.JobListener;

/**
 * @since 0.14
 */
public class JobModuleExtender {

    private Binder binder;
    private Multibinder<Job> jobs;
    private Multibinder<JobListener> listeners;

    JobModuleExtender(Binder binder) {
        this.binder = binder;
    }

    JobModuleExtender initAllExtensions() {
        contributeListeners();
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
}
