package pl.mielecmichal.filesystemmonitor;

import io.vavr.collection.Array;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pl.mielecmichal.filesystemmonitor.parameters.ModificationStrategy;
import pl.mielecmichal.filesystemmonitor.parameters.PathKind;
import pl.mielecmichal.filesystemmonitor.utilities.AwaitilityUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.mielecmichal.filesystemmonitor.FilesystemEventType.INITIAL;

class RecursivePathTest {

	@ParameterizedTest
	@MethodSource
	void shouldWatchModifiedFile(PathKind pathKind, ModificationStrategy strategy, @TempDir Path temporaryDirectory) throws InterruptedException {
		//given
		List<Path> recursivePaths = pathKind.apply(temporaryDirectory);
		Path recursivePath = recursivePaths.get(recursivePaths.size() - 1);

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
		Path modifiedPath = strategy.apply(recursivePath);

		//then
		AwaitilityUtils.awaitForSize(receivedEvents, pathKind.getNumberOfPaths() + 1);
		Stream<FilesystemEvent> initial = recursivePaths.stream().map(path -> FilesystemEvent.of(path, INITIAL));
		Stream<FilesystemEvent> modified = Stream.of(FilesystemEvent.of(modifiedPath, strategy.getExpectedEvent()));
		List<FilesystemEvent> expected = Stream.concat(initial, modified).collect(Collectors.toList());
		Assertions.assertThat(receivedEvents).containsExactlyInAnyOrderElementsOf(expected);
	}

	private static Stream<Arguments> shouldWatchModifiedFile() {
		Array<ModificationStrategy> modificationStrategies = Array.of(ModificationStrategy.values());
		Array<PathKind> pathKinds = Array.of(PathKind.values());

		var arguments = pathKinds.crossProduct(modificationStrategies);
		return arguments.toJavaStream().map(tuple -> Arguments.of(tuple._1(), tuple._2()));
	}
}
