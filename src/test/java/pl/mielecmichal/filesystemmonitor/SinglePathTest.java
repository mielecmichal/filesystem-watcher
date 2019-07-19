package pl.mielecmichal.filesystemmonitor;


import io.vavr.collection.Array;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pl.mielecmichal.filesystemmonitor.parameters.ModificationStrategy;
import pl.mielecmichal.filesystemmonitor.parameters.PathKind;
import pl.mielecmichal.filesystemmonitor.utilities.AwaitilityUtils;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static pl.mielecmichal.filesystemmonitor.FilesystemEventType.*;

class SinglePathTest {

    @Test
    void shouldReadInitialFile(@TempDir Path temporaryFolder) {
        //given
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        Path testFile = FilesystemUtils.createFile(temporaryFolder, "test.txt");
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConstraints(FilesystemConstraints.DEFAULT)
                .watchedConsumer(receivedEvents::add)
                .build();

        //when
        monitor.startWatching();

        //then
        AwaitilityUtils.awaitForSize(receivedEvents, 1);
        Assertions.assertThat(receivedEvents).containsExactly(FilesystemEvent.of(testFile, INITIAL));
    }

    @Test
    void shouldWatchCreatedFile(@TempDir Path temporaryFolder) {
        //given
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConstraints(FilesystemConstraints.DEFAULT)
                .watchedConsumer(receivedEvents::add)
                .build();

        //when
        monitor.startWatching();
        Path testFile = FilesystemUtils.createFile(temporaryFolder, "test.txt");

        //then
        AwaitilityUtils.awaitForSize(receivedEvents, 1);
        Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath).containsExactly(testFile);
    }

    @Test
    void shouldWatchDeletedFile(@TempDir Path temporaryFolder) {
        //given
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());

        Path testFile = FilesystemUtils.createFile(temporaryFolder, "test.txt");
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConstraints(FilesystemConstraints.DEFAULT)
                .watchedConsumer(receivedEvents::add)
                .build();

        //when
        monitor.startWatching();
        FilesystemUtils.deleteFile(testFile);

        //then
        AwaitilityUtils.awaitForSize(receivedEvents, 2);
        Assertions.assertThat(receivedEvents).contains(
                FilesystemEvent.of(testFile, INITIAL),
                FilesystemEvent.of(testFile, DELETED)
        );
    }

    @ParameterizedTest
    @MethodSource
    void shouldWatchModifiedFile(PathKind pathKind, ModificationStrategy modificationStrategy, @TempDir Path temporaryFolder) {
        //given
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        Path path = pathKind.apply(temporaryFolder);
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConstraints(FilesystemConstraints.DEFAULT)
                .watchedConsumer(receivedEvents::add)
                .build();
        //when
        monitor.startWatching();
        modificationStrategy.accept(path);

        //then
        AwaitilityUtils.awaitForSize(receivedEvents, 2);
        Assertions.assertThat(receivedEvents).contains(
                FilesystemEvent.of(path, INITIAL),
                FilesystemEvent.of(path, MODIFIED)
        );
    }

    private static Stream<Arguments> shouldWatchModifiedFile() {
        Array<ModificationStrategy> modificationStrategies = Array.of(ModificationStrategy.values());
        Array<PathKind> pathKinds = Array.of(PathKind.values());

        var arguments = pathKinds.crossProduct(modificationStrategies);
        return arguments.toJavaStream().map(tuple -> Arguments.of(tuple._1(), tuple._2()));
    }
}