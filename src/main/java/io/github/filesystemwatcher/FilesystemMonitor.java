package io.github.filesystemwatcher;

import lombok.Builder;
import lombok.experimental.NonFinal;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Builder
public class FilesystemMonitor implements FilesystemNotifier {

    private final Path watchedPath;
    private final Consumer<FilesystemEvent> watchedConsumer;
    private final FilesystemConstraints watchedConstraints;

    private final BlockingQueue<FilesystemEvent> queue = new ArrayBlockingQueue<>(100000);
    private final ExecutorService producersExecutor = Executors.newSingleThreadExecutor(
            new FilesystemMonitorThreadFactory(getClass().getSimpleName() + "Producers")
    );
    private final ExecutorService consumersExecutor = Executors.newSingleThreadExecutor(
            new FilesystemMonitorThreadFactory(getClass().getSimpleName() + "Consumers")
    );

    @NonFinal
    private FilesystemNotifier watcher;
    @NonFinal
    private FilesystemNotifier reader;

    @Override
    public void startWatching() {

        watcher = FilesystemWatcher.builder()
                .watchedPath(watchedPath)
                .watchedConstraints(watchedConstraints)
                .watchedConsumer(watchedConsumer)
                .producersExecutor(producersExecutor)
                .consumersExecutor(consumersExecutor)
                .blockingQueue(queue)
                .build();

        reader = FilesystemReader.builder()
                .watchedPath(watchedPath)
                .watchedConstraints(watchedConstraints)
                .watchedConsumer(this::consumeEvent)
                .build();

        watcher.startWatching();
        reader.startWatching();
    }

    @Override
    public void stopWatching() {
        reader.stopWatching();
        watcher.stopWatching();
    }

    private void consumeEvent(FilesystemEvent event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
