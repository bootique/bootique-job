package io.bootique.job.scheduler;

import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJob;
import io.bootique.job.runnable.RunnableJobFactory;
import org.springframework.scheduling.Trigger;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

class ScheduledJob implements Job {

    private Job delegate;
    private RunnableJobFactory runnableJobFactory;
    private DefaultScheduler scheduler;

    ScheduledJob(Job delegate, RunnableJobFactory runnableJobFactory, DefaultScheduler scheduler) {
        this.delegate = delegate;
        this.runnableJobFactory = runnableJobFactory;
        this.scheduler = scheduler;
    }

    public JobMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public JobResult run(Map<String, Object> parameters) {
        return delegate.run(parameters);
    }

    public ScheduledFuture<?> schedule(Map<String, Object> parameters, Trigger trigger) {
        RunnableJob rj = runnableJobFactory.runnable(delegate, parameters);
        return scheduler.schedule(rj::run, delegate.getMetadata(), trigger);
    }

    public JobFuture runAsync(Map<String, Object> parameters) {
        return runJobWithParameters(delegate, parameters, new Date());
    }

    private JobFuture runJobWithParameters(Job job, Map<String, Object> parameters, Date date) {
        if (job instanceof LazyJobGroup) {
            // do not create an actual job for the containing group
            // TODO: in fact it would be nice to not waste 1 thread on the group runner
            return scheduler.runOnce(() -> job.run(parameters), job.getMetadata(), date);
        } else {
            RunnableJob rj = runnableJobFactory.runnable(job, parameters);
            return scheduler.runOnce(rj::run, job.getMetadata(), date);
        }
    }
}
