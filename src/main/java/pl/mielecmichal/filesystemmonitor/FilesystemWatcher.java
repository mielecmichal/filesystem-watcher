package pl.mielecmichal.filesystemmonitor;

import io.vavr.control.Try;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.nio.file.StandardWatchEventKinds.*;
import static pl.mielecmichal.filesystemmonitor.FilesystemEventType.*;

@Slf4j
@Builder
@Value
public class FilesystemWatcher implements FilesystemNotifier {

    private final WatchableUtility watchableUtility = new WatchableUtility();

    private final Path watchedPath;
    private final FilesystemConstraints watchedConstraints;
    private final Consumer<FilesystemEvent> watchedConsumer;

    private final BlockingQueue<FilesystemEvent> blockingQueue;
    private final ExecutorService producersExecutor;
    private final ExecutorService consumersExecutor;

    private final Map<Path, WatchKey> watchedKeys = new ConcurrentHashMap<>();

    @NonFinal
    private Future<?> consumer;
    @NonFinal
    private Future<?> producer;

    @Override
    public void startWatching() {
        startWatching(watchedPath);
        consumer = consumersExecutor.submit(this::consumeEvents);
        producer = producersExecutor.submit(this::produceEvents);
    }

    @Override
    public void stopWatching() {
        try {
            watchableUtility.closeWatchService();
        } finally {
            blockingQueue.clear();
            producersExecutor.shutdownNow();
            consumersExecutor.shutdownNow();
        }
    }

    private void startWatching(Path path) {
        WatchKey key = watchableUtility.registerWatchable(path);
        watchedKeys.putIfAbsent(path, key);
        log.info("Watching started: {}", path);
    }

    private void stopWatching(Path path) {
        WatchKey key = watchedKeys.get(path);
        key.cancel();
        watchedKeys.remove(path);
        log.info("Watching stopped: {}", path);
    }

    private void produceEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey watchedKey = watchableUtility.getWatchService().take();
                log.info("Watched key: {}", watchedKey.watchable());

                for (WatchEvent<?> watchEvent : watchedKey.pollEvents()) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    log.info("Watched event: {} {}", watchEvent.kind(), watchEvent.context());

                    if (watchEvent.kind() == OVERFLOW) {
                        log.error("OVERFLOW watchEvent occurred {} times. Operating system queue was overflowed. File events could be lost.", watchEvent.count());
                        continue;
                    }

                    Path watchedDirectory = (Path) watchedKey.watchable();
                    FilesystemEvent filesystemEvent = FilesystemEvent.of(watchEvent, watchedDirectory);

                    if (!watchedConstraints.test(filesystemEvent)) {
                        continue;
                    }

                    blockingQueue.put(filesystemEvent);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    private void consumeEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                FilesystemEvent event = blockingQueue.take();

                Path path = event.getPath();
                if (Files.isDirectory(path)) {
                    if (List.of(INITIAL, CREATED).contains(event.getEventType())) {
                        startWatching(path);
                    } else if (event.getEventType() == DELETED) {
                        stopWatching(path);
                    }
                }
                log.info("Consumed event: " + event);

                watchedConsumer.accept(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Value
    private static class WatchableUtility {
        private static final WatchEvent.Kind[] ALL_EVENT_KINDS = new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW};

        private final Supplier<IllegalStateException> exceptionSupplier = IllegalStateException::new;
        private final WatchService watchService = createWatchService();

        private WatchService createWatchService() {
            FileSystem fileSystem = FileSystems.getDefault();
            return Try.of(fileSystem::newWatchService).getOrElseThrow(exceptionSupplier);
        }

        private WatchKey registerWatchable(Watchable watchable) {
            return Try.of(() -> watchable.register(watchService, ALL_EVENT_KINDS)).getOrElseThrow(exceptionSupplier);
        }

        void closeWatchService() {
            Try.run(watchService::close).getOrElseThrow(exceptionSupplier);
        }
    }
}
