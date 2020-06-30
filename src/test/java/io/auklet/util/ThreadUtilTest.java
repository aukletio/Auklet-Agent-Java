package io.auklet.util;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadUtilTest {
    @Test void testCreateDaemonThreadFactory() {
        assertNotNull(ThreadUtil.createDaemonThreadFactory("0"));
    }

    @Test void testShutdown() {
        ExecutorService es = new ExecutorService() {
            Boolean isShutdownBool = true;

            @Override
            public void shutdown() {
                isShutdownBool = false;
            }

            @Override
            public List<Runnable> shutdownNow() {
                return null;
            }

            @Override
            public boolean isShutdown() {
                return isShutdownBool;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return false;
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                return null;
            }

            @Override
            public <T> Future<T> submit(Runnable task, T result) {
                return null;
            }

            @Override
            public Future<?> submit(Runnable task) {
                return null;
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                return null;
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
                return null;
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }

            @Override
            public void execute(Runnable command) {

            }
        };
        ThreadUtil.shutdown(es);
        assertFalse(es.isShutdown());
    }
}
