package pl.mielecmichal.filesystemmonitor;

import io.vavr.control.Try;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.nio.file.StandardWatchEventKinds.*;

@Log
@Builder
@Value
public class NioFilesystemWatcher implements FilesystemWatcher {

    private static final WatchEvent.Kind[] ALL_WATCH_KINDS = new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW};

    private final Path watchedPath;
    private final FilesystemConstraints watchedConstraints;
    private final Consumer<FilesystemEvent> watchedConsumer;

    private final BlockingQueue<FilesystemEvent> blockingQueue = new ArrayBlockingQueue<>(100000);
    private final ExecutorService producersExecutor;
    private final ExecutorService consumersExecutor;

    private WatchService watcher = Try.of(() -> {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        log.info("CREATED" + watchService); return watchService;}).getOrElseThrow((Function<Throwable, IllegalStateException>) IllegalStateException::new);
    @NonFinal
    private Future<?> consumer;
    @NonFinal
    private Future<?> producer;

    @Override
    public void startWatching() {
        Try.of(() -> watchedPath.register(watcher, ALL_WATCH_KINDS)).getOrElseThrow((Function<Throwable, IllegalStateException>) IllegalStateException::new);
        log.info("STARTED" + watchedPath);
        consumer = consumersExecutor.submit(() -> consumeEvents());
        producer = producersExecutor.submit(() -> produceEvents());
    }

    @Override
    public void stopWatching() {
        try {
            watcher.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            blockingQueue.clear();
            producersExecutor.shutdownNow();
            consumersExecutor.shutdownNow();
        }
    }

    private void produceEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            log.info("PRODUCE" + watchedPath);

            try {
                WatchKey take = watcher.take();
                log.info(watchedPath + "PRODUCED " + take);

                for (WatchEvent<?> watchEvent : take.pollEvents()) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    log.fine(String.format("Found startWatching event = %s", watchEvent));

                    if (watchEvent.kind() == OVERFLOW) {
                        log.severe(String.format("OVERFLOW watchEvent occurred %s times. Operating system queue was overflowed. File events could be lost.", watchEvent.count()));
                        continue;
                    }

                    Path filePath = (Path) watchEvent.context();
                    Path path = Paths.get(watchedPath.toString(), filePath.toString());

                    FilesystemEvent filesystemEvent = FilesystemEvent.builder()
                            .eventType(FilesystemEvent.FilesystemEventType.of(watchEvent.kind()))
                            .path(path)
                            .build();


                    if (watchedConstraints.test(filesystemEvent)) {
                        log.info("Found " + filesystemEvent);
                        blockingQueue.put(filesystemEvent);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.severe(e.getMessage());
            }
        }
    }

    private void consumeEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                FilesystemEvent event = blockingQueue.take();
                log.info(watchedPath + "CONSUMED " + event);
                watchedConsumer.accept(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


}
