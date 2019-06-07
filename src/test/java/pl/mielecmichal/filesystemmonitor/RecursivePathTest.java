package pl.mielecmichal.filesystemmonitor;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.mielecmichal.filesystemmonitor.parameters.ModificationKind;
import pl.mielecmichal.filesystemmonitor.utilities.Filesystem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static pl.mielecmichal.filesystemmonitor.Constants.TEST_TIMEOUT;
import static pl.mielecmichal.filesystemmonitor.FilesystemEvent.FilesystemEventType.*;

class RecursivePathTest {

	@ParameterizedTest
	@EnumSource(ModificationKind.class)
	void shouldWatchRecursiveFiles(ModificationKind modificationKind, @TempDir Path temporaryDirectory) throws InterruptedException {
		//given
		Path recursive = Filesystem.createDirectory(temporaryDirectory, "recursive");
		Path recursiveFile = Filesystem.createFile(recursive, "recursive.txt");

		//when
		CountDownLatch countDownLatch = new CountDownLatch(3);
		List<FilesystemEvent> receivedEvents = new ArrayList<>();
		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryDirectory)
				.watchedConsumer(event -> {
					receivedEvents.add(event);
					countDownLatch.countDown();
				})
				.build();

		//when
		monitor.startWatching();
		modificationKind.accept(recursiveFile);
		countDownLatch.await(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

		//then
		Assertions.assertThat(receivedEvents).hasSize(3);
		Assertions.assertThat(receivedEvents).containsExactly(
				FilesystemEvent.builder().path(recursive).eventType(INITIAL).build(),
				FilesystemEvent.builder().path(recursiveFile).eventType(INITIAL).build(),
				FilesystemEvent.builder().path(recursiveFile).eventType(MODIFIED).build()
		);
	}

	@Test
	void shouldWatchRecursiveDirectories() {


	}
}
