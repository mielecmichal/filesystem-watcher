package pl.mielecmichal.filesystemmonitor;

import com.sun.nio.file.SensitivityWatchEventModifier;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

@Slf4j
class WatchServiceTest {
    private static final WatchEvent.Kind[] ALL_EVENT_KINDS = new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW};
    private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

    private static CountDownLatch startWatching(WatchService watchService, List<WatchEvent> observedEvents) {
        CountDownLatch initializationLatch = new CountDownLatch(1);
        Executors.newSingleThreadExecutor().submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    log.info("Watching started");
                    initializationLatch.countDown();
                    WatchKey take = watchService.take();
                    List<WatchEvent<?>> events = take.pollEvents();
                    events.forEach(event -> log.info("Event: kind={} path={} count={}", event.kind(), event.context(), event.count()));
                    observedEvents.addAll(events);
                    take.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }
        });
        return initializationLatch;
    }

    @RepeatedTest(20)
    void shouldObserveSymlinksWithoutTryIO(@TempDir Path temporaryDirectory) throws IOException, InterruptedException {
        WatchService watchService = FILE_SYSTEM.newWatchService();
        temporaryDirectory.register(watchService, ALL_EVENT_KINDS, SensitivityWatchEventModifier.HIGH);

        List<WatchEvent> observedEvents = new CopyOnWriteArrayList<>();
        CountDownLatch initializationLatch = startWatching(watchService, observedEvents);
        initializationLatch.await();

        Path file = Files.createFile(temporaryDirectory.resolve("test.txt"));
        Path linkPath = Files.createSymbolicLink(temporaryDirectory.resolve("symlink"), file);
        log.info("{} {}", file, linkPath);

        Awaitility.await().atMost(Duration.TWO_HUNDRED_MILLISECONDS).until(() -> observedEvents.size() == 2);
    }

    @RepeatedTest(20)
    void shouldObserveSymlinksWithLogging(@TempDir Path temporaryDirectory) throws IOException, InterruptedException {
        WatchService watchService = FILE_SYSTEM.newWatchService();
        temporaryDirectory.register(watchService, ALL_EVENT_KINDS, SensitivityWatchEventModifier.HIGH);

        List<WatchEvent> observedEvents = new CopyOnWriteArrayList<>();
        CountDownLatch initializationLatch = startWatching(watchService, observedEvents);
        initializationLatch.await();

        Path file = TryIO.with(() -> {
            Path file1 = Files.createFile(temporaryDirectory.resolve("test.txt"));
            log.info("Created file: {}", file1);
            return file1;
        });
        Path linkPath = TryIO.with(() -> {
            Path symlink = Files.createSymbolicLink(temporaryDirectory.resolve("symlink"), file);
            log.info("Created symlink: {}", symlink);
            return symlink;
        });

        log.info("{} {}", file, linkPath);

        Awaitility.await().atMost(Duration.TWO_HUNDRED_MILLISECONDS).until(() -> observedEvents.size() == 2);
    }

    static class TryIO {

        static <T> T with(TryIO.ThrowingSupplier<T> throwingSupplier) {
            Supplier<T> supplier = () -> {
                try {
                    return throwingSupplier.apply();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            return supplier.get();
        }

        @FunctionalInterface
        interface ThrowingSupplier<T> {
            T apply() throws IOException;
        }
    }

    @RepeatedTest(20)
    void shouldObserveSymlinksWithoutLogging(@TempDir Path temporaryDirectory) throws IOException, InterruptedException {
        FileSystem fileSystem = FileSystems.getDefault();
        WatchService watchService = fileSystem.newWatchService();
        temporaryDirectory.register(watchService, ALL_EVENT_KINDS, SensitivityWatchEventModifier.HIGH);
        List<WatchEvent> observedEvents = new CopyOnWriteArrayList<>();

        CountDownLatch initializationLatch = startWatching(watchService, observedEvents);
        initializationLatch.await();

        Path file = TryIO.with(() -> Files.createFile(temporaryDirectory.resolve("test.txt")));
        Path linkPath = TryIO.with(() -> Files.createSymbolicLink(temporaryDirectory.resolve("symlink"), file));
        log.info("{} {}", file, linkPath);

        Awaitility.await().atMost(Duration.TWO_HUNDRED_MILLISECONDS).until(() -> observedEvents.size() == 2);
    }
}
