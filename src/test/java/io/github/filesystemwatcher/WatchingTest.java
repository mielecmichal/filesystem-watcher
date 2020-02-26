package io.github.filesystemwatcher;

import io.github.filesystemwatcher.parameters.ModificationKind;
import io.github.filesystemwatcher.parameters.PathKind;
import io.github.filesystemwatcher.utilities.AwaitilityUtils;
import io.github.filesystemwatcher.utilities.FilesystemUtils;
import io.vavr.collection.Array;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.filesystemwatcher.FilesystemEventType.*;

@Slf4j
class WatchingTest {

    private static Stream<Arguments> allPathKinds() {
        return Arrays.stream(PathKind.values()).map(Arguments::of);
    }

    private static Stream<Arguments> allModificationKindsOnAllPathKinds() {
        Array<PathKind> pathKinds = Array.of(PathKind.values());
        Array<ModificationKind> modificationKinds = Array.of(ModificationKind.values());

        var arguments = pathKinds.crossProduct(modificationKinds);
        return arguments.toJavaStream().map(tuple -> Arguments.of(tuple._1(), tuple._2()));
    }

    @Test
    void shouldNotEmitEventsForEmptyDirectory(@TempDir Path temporaryFolder) throws InterruptedException {
        //given
        List<FilesystemEvent> receivedEvents = new ArrayList<>();
        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConsumer(receivedEvents::add)
                .build();

        //when
        monitor.startWatching();
        //TODO Monitor should initialize recursive directory watchers if possible before exiting from startWatching method.
        Thread.sleep(100);

        //then
        Assertions.assertThat(receivedEvents).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("allPathKinds")
    void shouldWatchCreations(PathKind pathKind, @TempDir Path temporaryDirectory) throws InterruptedException {
        //given
        ConcurrentHashMap<UUID, FilesystemEvent> receivedEvents = new ConcurrentHashMap<>();
        FilesystemNotifier monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryDirectory)
                .watchedConstraints(FilesystemConstraints.DEFAULT.withRecursive(true))
                .watchedConsumer(filesystemEvent -> {
                    log.info("ADDED: {}", filesystemEvent);
                    receivedEvents.put(UUID.randomUUID(), filesystemEvent);
                })
                .build();
        monitor.startWatching();

        //when
        PathKind.PathScenario setup = pathKind.apply(temporaryDirectory);
        List<Path> createdPaths = setup.getAllPaths();
        //TODO Monitor should initialize recursive directory watchers if possible before exiting from startWatching method.
        Thread.sleep(100);

        //then
        AwaitilityUtils.awaitForSize(receivedEvents, createdPaths.size());
        List<FilesystemEvent> createdEvents = createdPaths.stream()
                .map(path -> FilesystemEvent.of(path, CREATED))
                .collect(Collectors.toList());
        Assertions.assertThat(receivedEvents.values()).containsExactlyInAnyOrderElementsOf(createdEvents);
    }

    @ParameterizedTest
    @MethodSource("allPathKinds")
    void shouldWatchDeletions(PathKind pathKind, @TempDir Path temporaryDirectory) throws InterruptedException {
        //given
        PathKind.PathScenario setup = pathKind.apply(temporaryDirectory);
        Path subjectPath = setup.getSubjectPath();

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

        FilesystemUtils.delete(subjectPath);

        //then
        AwaitilityUtils.awaitForSize(receivedEvents, setup.getAllPaths().size() + 1);
        Stream<FilesystemEvent> initial = setup.getAllPaths().stream().map(path -> FilesystemEvent.of(path, INITIAL));
        Stream<FilesystemEvent> modified = Stream.of(FilesystemEvent.of(setup.getSubjectPath(), DELETED));
        List<FilesystemEvent> expected = Stream.concat(initial, modified).collect(Collectors.toList());
        Assertions.assertThat(receivedEvents).containsExactlyInAnyOrderElementsOf(expected);
    }

    @ParameterizedTest
    @EnabledOnOs({OS.LINUX, OS.WINDOWS})
    @MethodSource("allModificationKindsOnAllPathKinds")
    void shouldWatchModifications(PathKind pathKind, ModificationKind strategy, @TempDir Path temporaryDirectory) throws InterruptedException {
        //given
        PathKind.PathScenario setup = pathKind.apply(temporaryDirectory);
        Path testedPath = setup.getTestedPath();

        //when
        List<FilesystemEvent> receivedEvents = new CopyOnWriteArrayList<>();
        FilesystemNotifier monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryDirectory)
                .watchedConstraints(FilesystemConstraints.DEFAULT.withRecursive(true))
                .watchedConsumer(filesystemEvent -> {
                    log.info("Event: {}", filesystemEvent);
                    receivedEvents.add(filesystemEvent);
                })
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
