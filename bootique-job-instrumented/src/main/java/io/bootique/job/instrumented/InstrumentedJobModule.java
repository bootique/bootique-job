package io.bootique.job.instrumented;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.ConfigModule;
import io.bootique.job.MappedJobListener;
import io.bootique.job.runtime.JobModule;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;

import static io.bootique.job.runtime.JobModule.BUSINESS_TX_LISTENER_ORDER;
import static io.bootique.job.runtime.JobModule.LOG_LISTENER_ORDER;

/**
 * @since 0.14
 */
public class InstrumentedJobModule extends ConfigModule {

    public static final int JOB_LISTENER_ORDER = LOG_LISTENER_ORDER + 200;

    public InstrumentedJobModule() {

    }

    public InstrumentedJobModule(String configPrefix) {
        super(configPrefix);
    }

    @Override
    public void configure(Binder binder) {
        JobModule.extend(binder)
                .addMappedListener(new TypeLiteral<MappedJobListener<InstrumentedJobListener>>() {
                })
                .addMappedListener(new TypeLiteral<MappedJobListener<JobMDCManager>>() {
                });
    }

    @Provides
    @Singleton
    public MappedJobListener<InstrumentedJobListener> provideInstrumentedJobListener(MetricRegistry metricRegistry) {
        return new MappedJobListener<>(new InstrumentedJobListener(metricRegistry), JOB_LISTENER_ORDER);
    }

    @Provides
    @Singleton
    MappedJobListener<JobMDCManager> provideJobMDCManager(TransactionIdGenerator generator, TransactionIdMDC mdc) {
        JobMDCManager mdcManager = new JobMDCManager(generator, mdc);
        return new MappedJobListener<>(mdcManager, BUSINESS_TX_LISTENER_ORDER);
    }
}
