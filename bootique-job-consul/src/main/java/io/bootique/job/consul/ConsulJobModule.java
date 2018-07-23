package io.bootique.job.consul;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.job.consul.lock.ConsulLockHandlerProvider;
import io.bootique.job.lock.LockHandler;
import io.bootique.shutdown.ShutdownManager;

/**
 * @since 0.26
 */
public class ConsulJobModule extends ConfigModule {

    public ConsulJobModule() {
    }

    public ConsulJobModule(String configPrefix) {
        super(configPrefix);
    }

    @Override
    protected String defaultConfigPrefix() {
        return "job-consul";
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Key.get(LockHandler.class)).toProvider(ConsulLockHandlerProvider.class);
    }

    @Provides
    @Singleton
    public ConsulLockHandlerProvider provideConsulLockHandlerProvider(
            ConfigurationFactory configFactory, ShutdownManager shutdownManager) {

        ConsulJobConfig config = configFactory.config(ConsulJobConfig.class, configPrefix);

        return new ConsulLockHandlerProvider(
                config.getConsulHost(),
                config.getConsulPort(),
                config.getDataCenter(),
                config.getServiceGroup(),
                shutdownManager);
    }
}
