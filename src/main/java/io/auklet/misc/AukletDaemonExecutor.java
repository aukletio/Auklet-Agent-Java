package io.auklet.misc;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * <p>Auklet daemon task executor that logs exceptions that occur in submitted tasks.</p>
 *
 * <p>To prevent an infinite loop, exceptions that are logged by this executor are not submitted
 * to the Auklet data sink and are only logged to SLF4J.</p>
 */
@ThreadSafe
public final class AukletDaemonExecutor extends ScheduledThreadPoolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AukletDaemonExecutor.class);
    private final Object lock = new Object();
    private boolean logCancelExceptions = true;

    /**
     * Constructor.
     *
     * @param corePoolSize the number of threads in this executor.
     * @param threadFactory the thread factory to use.
     */
    public AukletDaemonExecutor(int corePoolSize, @NonNull ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    /**
     * <p>Configures the executor to enable/disable logging of {@link CancellationException}s.</p>
     *
     * @param enabled {@code true} to log these exceptions, {@code false} to skip logging.
     */
    public void logCancelExceptions(boolean enabled) {
        synchronized(lock) { logCancelExceptions = enabled; }
    }

    /* Logs exceptions that occur in tasks. */
    @Override protected void afterExecute(@Nullable Runnable r, @Nullable Throwable t) {
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
        if (t instanceof CancellationException) {
            boolean logThis;
            synchronized(lock) { logThis = logCancelExceptions; }
            if (logThis) LOGGER.warn("Auklet daemon task cancelled.", t);
        }
        else if (t != null) LOGGER.warn("Exception in Auklet daemon task.", t);
    }

    /* Decorates CancelSilentlyFutureTasks so that afterExecute() knows about them. */
    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(
            @Nullable Runnable r, @NonNull RunnableScheduledFuture<V> task) {
        if (task == null) throw new IllegalArgumentException("Task is null.");
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
        private CancelSilentlyRSF(@NonNull RunnableScheduledFuture<V> task) {
            if (task == null) throw new IllegalArgumentException("Task is null");
            this.task = task;
        }
        @Override
        public boolean isPeriodic() { return task.isPeriodic(); }
        @Override
        public long getDelay(@Nullable TimeUnit unit) { return task.getDelay(unit); }
        @Override
        public int compareTo(@Nullable Delayed o) { return task.compareTo(o); }
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
        public V get(long timeout, @Nullable TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }
    }

}
