package io.bootique.job.runtime;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.curator.CuratorModuleProvider;
import io.bootique.job.config.JobDefinition;
import io.bootique.job.scheduler.SchedulerFactory;
import io.bootique.type.TypeRef;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JobModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new JobModule();
    }

    @Override
    public Map<String, Type> configs() {

        TypeRef<Map<String,JobDefinition>> jobs = new TypeRef<Map<String,JobDefinition>>() {};

        Map<String, Type> configs = new HashMap<>();
        configs.put("scheduler", SchedulerFactory.class);
        configs.put("jobs", jobs.getType());
        return configs;
    }

    @Override
    public Collection<BQModuleProvider> dependencies() {
        return Collections.singletonList(new CuratorModuleProvider());
    }
}
