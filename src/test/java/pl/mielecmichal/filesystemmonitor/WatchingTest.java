package pl.mielecmichal.filesystemmonitor;

import io.vavr.collection.Array;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pl.mielecmichal.filesystemmonitor.parameters.ModificationKind;
import pl.mielecmichal.filesystemmonitor.parameters.PathKind;
import pl.mielecmichal.filesystemmonitor.utilities.AwaitilityUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.mielecmichal.filesystemmonitor.FilesystemEventType.INITIAL;

class WatchingTest {

    private static Stream<Arguments> shouldWatchModifiedFile() {
        Array<PathKind> pathKinds = Array.of(PathKind.values());
        Array<ModificationKind> modificationKinds = Array.of(ModificationKind.values());

        var arguments = pathKinds.crossProduct(modificationKinds);
        return arguments.toJavaStream().map(tuple -> Arguments.of(tuple._1(), tuple._2()));
    }

    @Test
    void shouldNotEmitEventsForEmptyDirectory(@TempDir Path temporaryFolder) {
        //given
        List<FilesystemEvent> receivedEvents = new ArrayList<>();
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConsumer(receivedEvents::add)
                .build();

        //when
        monitor.startWatching();

        //then
        Assertions.assertThat(receivedEvents).isEmpty();
    }

    @ParameterizedTest
    @MethodSource
    void shouldWatchModifiedFile(PathKind pathKind, ModificationKind strategy, @TempDir Path temporaryDirectory) throws InterruptedException {
        //given
        PathKind.PathScenario setup = pathKind.apply(temporaryDirectory);
        Path testedPath = setup.getTestedPath();

        //when
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        FilesystemNotifier monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryDirectory)
                .watchedConstraints(FilesystemConstraints.DEFAULT.withRecursive(true))
                .watchedConsumer(receivedEvents::add)
                .build();

        //when
        monitor.startWatching();
        //TODO Monitor should initialize recursive directory watchers if possible before exiting from startWatching method.
        Thread.sleep(100);
        strategy.apply(testedPath);

        //then
        AwaitilityUtils.awaitForSize(receivedEvents, setup.getAllPaths().size() + 1);
        Stream<FilesystemEvent> initial = setup.getAllPaths().stream().map(path -> FilesystemEvent.of(path, INITIAL));
        Stream<FilesystemEvent> modified = Stream.of(FilesystemEvent.of(setup.getSubjectPath(), strategy.getExpectedEvent()));
        List<FilesystemEvent> expected = Stream.concat(initial, modified).collect(Collectors.toList());
        Assertions.assertThat(receivedEvents).containsExactlyInAnyOrderElementsOf(expected);
    }
}
