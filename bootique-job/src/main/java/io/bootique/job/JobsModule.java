/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.job;

import io.bootique.BQModuleProvider;
import io.bootique.ModuleCrate;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.job.graph.JobGraphNode;
import io.bootique.job.graph.JobGraphNodeFactory;
import io.bootique.job.lock.LocalLockHandler;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.runtime.*;
import io.bootique.type.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;

/**
 * @since 3.0
 */
public class JobsModule implements BQModule, BQModuleProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsModule.class);

    private static final TypeRef<Map<String, JobGraphNodeFactory>> CONFIG_PROTO = new TypeRef<>() {
    };

    static final String CONFIG_PREFIX = "jobs";

    /**
     * Returns an instance of {@link JobsModuleExtender} used by downstream modules to load custom job-related extensions,
     * specifically to load job classes. Should be invoked from a custom module {@link BQModule#configure(Binder)} method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link JobsModuleExtender} that can be used to load custom extensions to the JobModule.
     */
    public static JobsModuleExtender extend(Binder binder) {
        return new JobsModuleExtender(binder);
    }

    @Override
    public ModuleCrate moduleCrate() {

        return ModuleCrate.of(this)
                .description("Loads jobs for Bootique job execution engine")
                .config(CONFIG_PREFIX, CONFIG_PROTO.getType())
                .build();
    }

    @Override
    public void configure(Binder binder) {
        JobsModule.extend(binder).initAllExtensions();
    }

    @Provides
    @Singleton
    Map<String, JobGraphNode> provideJobs(ConfigurationFactory configFactory, Set<Job> standaloneJobs) {
        // TODO: deserialize directly from YAML, bypassing TypeRef ?
        return new JobGraphNodesFactory(configFactory.config(CONFIG_PROTO, CONFIG_PREFIX)).create(standaloneJobs);
    }

    // TODO: due to the way our configuration suffixes are setup ("jobs" vs "scheduler"), service per-module allocation
    //   makes little sense.. All the services below are of no use outside of JobRegistry / Scheduler, but
    //   since they are attached to the JobsModuleExtender, they are declared here

    @Provides
    @Singleton
    JobDecorators provideDecorators(
            LockHandler lockHandler,
            JobLogger jobLogger,
            JobListenersDispatcherDecorator listenerDispatcher,
            Set<JobDecorator> decorators,
            Set<MappedJobDecorator<?>> mappedDecorators) {

        return JobDecorators.builder()
                .add(decorators)
                .addMapped(mappedDecorators)
                .exceptionHandler(new ExceptionsHandlerDecorator())
                .logger(jobLogger)
                .lockHandler(lockHandler)
                .listenerDispatcher(listenerDispatcher)
                .renamer(new JobNameDecorator())
                .paramsBinder(new JobParamsBinderDecorator())
                .create();
    }

    @Provides
    @Singleton
    LockHandler provideLockHandler(Set<LockHandler> lockHandlers) {
        switch (lockHandlers.size()) {
            case 0:
                // only use the default lock handler if none is provided by other modules
                return new LocalLockHandler();
            case 1:
                LOGGER.info("Using '{}' lock handler", lockHandlers.iterator().next());
                return lockHandlers.iterator().next();
            default:
                throw new RuntimeException("There's more than one LockHandler defined. Can't determine the default: " + lockHandlers);
        }
    }


    @Provides
    @Singleton
    JobLogger provideJobLogger() {
        return new JobLogger();
    }

    @Deprecated
    @Provides
    @Singleton
    JobListenersDispatcherDecorator provideListenerDecorator(Set<JobListener> listeners, Set<MappedJobListener> mappedListeners) {
        // not checking for dupes between MappedJobListener and JobListener collections. Is that a problem?
        List<MappedJobListener> localListeners = new ArrayList<>(mappedListeners.size() + listeners.size());
        localListeners.addAll(mappedListeners);

        //  Integer.MAX_VALUE means placing bare unordered listeners after (== inside) mapped listeners
        listeners.forEach(listener -> localListeners.add(new MappedJobListener<>(listener, Integer.MAX_VALUE)));
        localListeners.sort(Comparator.comparing(MappedJobListener::getOrder));

        return new JobListenersDispatcherDecorator(localListeners);
    }
}
