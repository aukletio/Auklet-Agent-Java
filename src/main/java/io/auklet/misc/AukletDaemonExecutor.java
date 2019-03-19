package io.auklet.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * <p>Auklet daemon task executor that logs exceptions that occur in submitted tasks.</p>
 *
 * <p>To prevent an infinite loop, exceptions that are logged by this executor are not submitted
 * to the Auklet data sink and are only logged to SLF4J.</p>
 */
public final class AukletDaemonExecutor extends ScheduledThreadPoolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AukletDaemonExecutor.class);

    /**
     * Constructor.
     *
     * @param corePoolSize the number of threads in this executor.
     * @param threadFactory the thread factory to use.
     */
    public AukletDaemonExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    /* Logs exceptions that occur in tasks. */
    @Override protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?>) {
            Future<?> future = (Future<?>) r;
            try {
                if (future.isDone()) future.get();
            } catch (CancellationException ce) {
                if (!(future instanceof CancelSilentlyRSF)) t = ce;
            } catch (ExecutionException ee) {
                t = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (t instanceof CancellationException) LOGGER.warn("Auklet daemon task cancelled.", t);
        else if (t != null) LOGGER.warn("Exception in Auklet daemon task.", t);
    }

    /* Decorates CancelSilentlyFutureTasks so that afterExecute() knows about them. */
    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(
            Runnable r, RunnableScheduledFuture<V> task) {
        return r instanceof CancelSilentlyRunnable ? new CancelSilentlyRSF<>(task) : task;
    }

    /**
     * A {@link Runnable} that the {@link AukletDaemonExecutor} will not log if it is cancelled.
     *
     * {@inheritDoc}
     */
    public abstract static class CancelSilentlyRunnable implements Runnable {}

    /*
     * Internal version of CancelSilentlyRunnable that is required by the
     * decorateTask() methods to pass to afterExecute() the fact that cancellation
     * should not be logged.
     */
    private static final class CancelSilentlyRSF<V> implements RunnableScheduledFuture<V> {
        private final RunnableScheduledFuture<V> task;
        private CancelSilentlyRSF(RunnableScheduledFuture<V> task) { this.task = task; }
        @Override
        public boolean isPeriodic() { return task.isPeriodic(); }
        @Override
        public long getDelay(TimeUnit unit) { return task.getDelay(unit); }
        @Override
        public int compareTo(Delayed o) { return task.compareTo(o); }
        @Override
        public void run() { task.run(); }
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) { return task.cancel(mayInterruptIfRunning); }
        @Override
        public boolean isCancelled() { return task.isCancelled(); }
        @Override
        public boolean isDone() { return task.isDone(); }
        @Override
        public V get() throws InterruptedException, ExecutionException { return task.get(); }
        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }
    }

}
