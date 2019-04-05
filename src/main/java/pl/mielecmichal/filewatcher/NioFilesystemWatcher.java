package pl.mielecmichal.filewatcher;

import io.vavr.control.Try;
import lombok.Builder;
import lombok.Value;
import lombok.extern.java.Log;

import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

@Log
@Builder
@Value
public class NioFilesystemWatcher implements FilesystemWatcher{

    private static final WatchEvent.Kind[] ALL_WATCH_KINDS = new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW};

    private final Path watchedPath;
    private final FilesystemConstraints watchedConstraints;
    private final Consumer<FilesystemEvent> watchedConsumer;

    private final BlockingQueue<FilesystemEvent> blockingQueue = new ArrayBlockingQueue<>(100000);
    private final ExecutorService producersExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService consumersExecutor = Executors.newSingleThreadExecutor();

    private WatchService watcher = Try.of(() -> FileSystems.getDefault().newWatchService()).getOrElseThrow(IllegalStateException::new);

    @Override
    public void watch() {
        Try.of(() -> watchedPath.register(watcher, ALL_WATCH_KINDS)).getOrElseThrow(IllegalStateException::new);
        consumersExecutor.submit(this::consumeEvents);
        producersExecutor.submit(this::produceEvents);
    }

    private void produceEvents() {
        try {
            WatchKey take = watcher.take();
            List<WatchEvent<?>> watchEvents = take.pollEvents();
            for (WatchEvent<?> watchEvent : watchEvents) {

                if (watchEvent.kind() == OVERFLOW) {
                    log.severe(String.format("OVERFLOW watchEvent occurred %s times. Operating system queue was overflowed. File events could be lost.", watchEvent.count()));
                    continue;
                }

                Path path = (Path) watchEvent.context();

                FilesystemEvent filesystemEvent = FilesystemEvent.builder()
                        .eventType(FilesystemEvent.FilesystemEventType.of(watchEvent.kind()))
                        .path(path)
                        .build();

                if (watchedConstraints.test(filesystemEvent)) {
                    blockingQueue.put(filesystemEvent);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void consumeEvents() {
        try {
            watchedConsumer.accept(blockingQueue.take());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


}
