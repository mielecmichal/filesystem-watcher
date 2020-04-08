package io.github.filesystemwatcher.threads;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

@RequiredArgsConstructor
public class LoggingExecutorService extends AbstractExecutorService {

    private final ExecutorService executorService;

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return super.newTaskFor(wrapCallable(callable));
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return super.newTaskFor(wrapTask(runnable), value);
    }

    private static <T> Callable<T> wrapCallable(Callable<T> callable) {
        Logger log = LoggerFactory.getLogger(callable.getClass());
        return () -> {
            try {
                return callable.call();
            } catch (Throwable throwable) {
                log.error("Exception caught in executor service:", throwable);
                throw throwable;
            }
        };
    }

    private static Runnable wrapTask(Runnable runnable) {
        Logger log = LoggerFactory.getLogger(runnable.getClass());
        return () -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                log.error("Exception caught in executor service:", throwable);
                throw throwable;
            }
        };
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executorService.execute(command);
    }
}
