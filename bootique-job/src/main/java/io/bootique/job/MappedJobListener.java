package io.bootique.job;

/**
 * A wrapper around {@link JobListener}  that contains an int ordering.
 * Lower ordering means an outer listener, higher - inner.
 *
 * @param <T>
 * @since 0.25
 */
public class MappedJobListener<T extends JobListener> {
    private T listener;
    private int order;

    public MappedJobListener(T listener, int order) {
        this.listener = listener;
        this.order = order;
    }

    public T getListener() {
        return listener;
    }

    public int getOrder() {
        return order;
    }
}