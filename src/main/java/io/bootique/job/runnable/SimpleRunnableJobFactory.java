package io.bootique.job.runnable;

import io.bootique.job.Job;
import io.bootique.job.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SimpleRunnableJobFactory implements RunnableJobFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRunnableJobFactory.class);

	private Set<JobListener> listeners;

    public SimpleRunnableJobFactory() {
        this.listeners = Collections.emptySet();
    }

    public SimpleRunnableJobFactory(Set<JobListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public RunnableJob runnable(Job delegate, Map<String, Object> parameters) {
        if (listeners.isEmpty()) {
            return () -> delegate.run(parameters);
        }

        String jobName = delegate.getMetadata().getName();
        return () -> {
            Callback callback = new Callback(jobName);
            listeners.forEach(listener -> {
				try {
					listener.onJobStarted(jobName, parameters, callback);
				} catch (Exception e) {
					LOGGER.error("Error invoking job listener for job: " + jobName, e);
				}
			});
            JobResult result;
            try {
                result = delegate.run(parameters);
            } catch (Exception e) {
                callback.invoke(JobResult.failure(delegate.getMetadata(), e));
                throw e;
            }
            callback.invoke(result);
            return result;
        };
    }

    private static class Callback implements Consumer<Consumer<JobResult>> {

        private static final Logger LOGGER = LoggerFactory.getLogger(Callback.class);

        private String jobName;
        private List<Consumer<JobResult>> callbacks;

        public Callback(String jobName) {
            this.jobName = jobName;
        }

        @Override
        public void accept(Consumer<JobResult> callback) {
            if (callbacks == null) {
                callbacks = new ArrayList<>();
            }
            callbacks.add(callback);
        }

        public void invoke(JobResult result) {
            callbacks.forEach(cb -> {
                try {
                    cb.accept(result);
                } catch (Exception e) {
                    LOGGER.error("Error invoking completion callback for job: " + jobName, e);
                }
            });
        }
    }
}
