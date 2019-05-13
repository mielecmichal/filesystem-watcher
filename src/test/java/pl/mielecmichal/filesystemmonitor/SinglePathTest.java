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

import static pl.mielecmichal.filesystemmonitor.FilesystemEvent.FilesystemEventType.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SinglePathTest {

	@TempDir
	Path temporaryFolder;

	@Test
	void shouldReadInitialFile() {
		//given
		Path testFile = Filesystem.createFile(temporaryFolder, "test.txt");
		List<FilesystemEvent> receivedEvents = new ArrayList<>();
		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryFolder)
				.watchedConsumer(receivedEvents::add)
				.build();

		//when
		monitor.startWatching();

		//then
		Assertions.assertThat(receivedEvents).hasSize(1);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath).containsExactly(testFile);
	}

	@Test
	void shouldWatchCreatedFile() throws InterruptedException {
		//given
		CountDownLatch countDownLatch = new CountDownLatch(1);
		List<FilesystemEvent> receivedEvents = new ArrayList<>();
		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryFolder)
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
				.watchedConsumer(event -> {
					receivedEvents.add(event);
					latch.countDown();
				})
				.build();

		//when
		monitor.startWatching();
		Filesystem.deleteFile(testFile);
		latch.await(2000, TimeUnit.MILLISECONDS);

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
		Path testFile = pathKind.apply(temporaryFolder);
		CountDownLatch latch = new CountDownLatch(2);
		List<FilesystemEvent> receivedEvents = new ArrayList<>();
		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryFolder)
				.watchedConsumer(event -> {
					receivedEvents.add(event);
					latch.countDown();
				})
				.build();
		//when
		monitor.startWatching();
		strategy.accept(testFile);
		latch.await(2000, TimeUnit.MILLISECONDS);

		//then
		Assertions.assertThat(receivedEvents).hasSize(1);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath)
				.contains(testFile, testFile);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getEventType)
				.contains(MODIFIED);
	}

	public Stream<Arguments> shouldWatchModifiedFile() {
		Array<ModificationKind> modificationStrategies = Array.of(ModificationKind.values());
		Array<PathKind> pathKinds = Array.of(PathKind.values());

		var arguments = pathKinds.crossProduct(modificationStrategies);
		return arguments.toJavaStream().map(tuple -> Arguments.of(tuple._1(), tuple._2()));
	}
}