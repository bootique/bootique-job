package io.bootique.job.instrumented;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.job.Job;
import io.bootique.job.JobListener;
import io.bootique.job.JobRegistry;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.scheduler.execution.DefaultJobRegistry;
import io.bootique.type.TypeRef;

import java.util.Map;
import java.util.Set;

/**
 * @since 0.14
 */
public class InstrumentedJobModule extends ConfigModule {

    public InstrumentedJobModule() {

    }

    public InstrumentedJobModule(String configPrefix) {
        super(configPrefix);
    }

    @Provides
    @Singleton
    protected JobRegistry createJobRegistry(Set<Job> jobs,
                                            Set<JobListener> jobListeners,
                                            Scheduler scheduler,
                                            ConfigurationFactory configFactory,
                                            MetricRegistry metricRegistry) {
        Map<String, JobDefinition> configuredDefinitions = configFactory.config(
                new TypeRef<Map<String, JobDefinition>>() {}, "jobs");

        jobListeners.add(new InstrumentedJobListener(metricRegistry));

        return new DefaultJobRegistry(jobs, configuredDefinitions, scheduler, jobListeners);
    }
}
