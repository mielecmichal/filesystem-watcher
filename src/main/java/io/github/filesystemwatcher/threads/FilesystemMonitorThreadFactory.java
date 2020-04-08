package io.github.filesystemwatcher.threads;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

@RequiredArgsConstructor
public class FilesystemMonitorThreadFactory implements ThreadFactory {

    private final String threadName;
    private long counter = 0;

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, threadName + "-" + counter);
        counter++;
        thread.setUncaughtExceptionHandler(createExceptionHandler(runnable));
        return thread;
    }

    private UncaughtExceptionHandler createExceptionHandler(Runnable runnable) {
        Logger logger = LoggerFactory.getLogger(runnable.getClass());
        return (thread, exception) -> logger.error("Uncaught exception in thread {}", thread, exception);
    }
}
