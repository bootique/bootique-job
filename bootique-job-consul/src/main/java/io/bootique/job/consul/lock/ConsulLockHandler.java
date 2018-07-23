package io.bootique.job.consul.lock;

import com.orbitz.consul.KeyValueClient;
import io.bootique.job.JobMetadata;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.runnable.JobOutcome;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class ConsulLockHandler implements LockHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulLockHandler.class);

    private final KeyValueClient kvClient;
    private final Supplier<String> consulSessionSupplier;
    private final String serviceGroup;

    public ConsulLockHandler(KeyValueClient kvClient, Supplier<String> consulSessionSupplier, String serviceGroup) {
        this.kvClient = kvClient;
        this.consulSessionSupplier = consulSessionSupplier;
        this.serviceGroup = serviceGroup;
    }

    @Override
    public RunnableJob lockingJob(RunnableJob executable, JobMetadata metadata) {
        return () -> {
            String lockName = getLockName(metadata);

            String sessionId = consulSessionSupplier.get();

            LOGGER.info("Attempting to lock '{}'", lockName);
            boolean acquired = kvClient.acquireLock(lockName, sessionId);
            if (!acquired) {
                LOGGER.info("** Another job instance owns the lock. Skipping execution of '{}'", lockName);
                return new JobResult(metadata, JobOutcome.SKIPPED, null,
                        "Another job instance owns the lock. Skipping execution");
            }

            try {
                return executable.run();
            } finally {
                if (!kvClient.releaseLock(lockName, consulSessionSupplier.get())) {
                    LOGGER.error("Failed to release lock, manual intervention might be needed: " + lockName);
                }
            }
        };
    }

    private String getLockName(JobMetadata metadata) {
        String jobName = metadata.getName();
        return (serviceGroup == null || serviceGroup.isEmpty()) ?
                jobName : (serviceGroup + "/" + jobName);
    }
}
