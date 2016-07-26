package io.bootique.job.runtime;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;

public class JobModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new JobModule();
    }
}
