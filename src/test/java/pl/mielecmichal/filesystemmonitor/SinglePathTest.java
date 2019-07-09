package pl.mielecmichal.filesystemmonitor;


import io.vavr.collection.Array;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pl.mielecmichal.filesystemmonitor.parameters.ModificationKind;
import pl.mielecmichal.filesystemmonitor.parameters.PathKind;
import pl.mielecmichal.filesystemmonitor.utilities.Filesystem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static pl.mielecmichal.filesystemmonitor.Constants.TEST_TIMEOUT;
import static pl.mielecmichal.filesystemmonitor.FilesystemEventType.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SinglePathTest {

    @TempDir
    Path temporaryFolder;

    @Test
    void shouldReadInitialFile() throws InterruptedException {
        //given
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Path testFile = Filesystem.createFile(temporaryFolder, "test.txt");
        List<FilesystemEvent> receivedEvents = new ArrayList<>();
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConstraints(FilesystemConstraints.DEFAULT)
                .watchedConsumer(filesystemEvent -> {
                    receivedEvents.add(filesystemEvent);
                    countDownLatch.countDown();
                })
                .build();

        //when
        monitor.startWatching();
        countDownLatch.await(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        //then
        Assertions.assertThat(receivedEvents).containsExactly(
                FilesystemEvent.of(INITIAL, testFile)
        );
    }

    @Test
    void shouldWatchCreatedFile() throws InterruptedException {
        //given
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<FilesystemEvent> receivedEvents = new ArrayList<>();
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConstraints(FilesystemConstraints.DEFAULT)
                .watchedConsumer(event -> {
                    receivedEvents.add(event);
                    countDownLatch.countDown();
                })
                .build();

        //when
        monitor.startWatching();
        Path testFile = Filesystem.createFile(temporaryFolder, "test.txt");
        countDownLatch.await(200, TimeUnit.MILLISECONDS);

        //then
        Assertions.assertThat(receivedEvents).hasSize(1);
        Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath).containsExactly(testFile);
    }

    @Test
    void shouldWatchDeletedFile() throws InterruptedException {
        //given
        Path testFile = Filesystem.createFile(temporaryFolder, "test.txt");

        CountDownLatch latch = new CountDownLatch(2);
        List<FilesystemEvent> receivedEvents = new ArrayList<>();
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConstraints(FilesystemConstraints.DEFAULT)
                .watchedConsumer(event -> {
                    receivedEvents.add(event);
                    latch.countDown();
                })
                .build();

        //when
        monitor.startWatching();
        Filesystem.deleteFile(testFile);
        latch.await(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        //then
        Assertions.assertThat(receivedEvents).hasSize(2);
        Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath)
                .contains(testFile, testFile);
        Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getEventType)
                .contains(INITIAL, DELETED);

    }

    @ParameterizedTest
    @MethodSource
    void shouldWatchModifiedFile(PathKind pathKind, ModificationKind strategy) throws InterruptedException {
        //given
        Path path = pathKind.apply(temporaryFolder);
        CountDownLatch latch = new CountDownLatch(2);
        List<FilesystemEvent> receivedEvents = new ArrayList<>();
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConstraints(FilesystemConstraints.DEFAULT)
                .watchedConsumer(event -> {
                    receivedEvents.add(event);
                    latch.countDown();
                })
                .build();
        //when
        monitor.startWatching();
        strategy.accept(path);
        latch.await(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        //then
        Assertions.assertThat(receivedEvents).hasSize(2);
        Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath)
                .contains(path, path);
        Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getEventType)
                .contains(INITIAL, MODIFIED);
    }

    public Stream<Arguments> shouldWatchModifiedFile() {
        Array<ModificationKind> modificationStrategies = Array.of(ModificationKind.values());
        Array<PathKind> pathKinds = Array.of(PathKind.values());

        var arguments = pathKinds.crossProduct(modificationStrategies);
        return arguments.toJavaStream().map(tuple -> Arguments.of(tuple._1(), tuple._2()));
    }
}