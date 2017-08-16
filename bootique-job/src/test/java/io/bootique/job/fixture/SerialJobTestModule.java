package io.bootique.job.fixture;

import com.google.inject.Binder;
import com.google.inject.Module;
import io.bootique.job.runtime.JobModule;

public class SerialJobTestModule implements Module {

    @Override
    public void configure(Binder binder) {
        JobModule.extend(binder).addJob(SerialJob1.class);
    }
}
