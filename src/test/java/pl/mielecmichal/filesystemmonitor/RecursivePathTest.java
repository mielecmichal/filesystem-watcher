package pl.mielecmichal.filesystemmonitor;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.mielecmichal.filesystemmonitor.parameters.ModificationKind;
import pl.mielecmichal.filesystemmonitor.utilities.Filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static pl.mielecmichal.filesystemmonitor.Constants.TEST_TIMEOUT;
import static pl.mielecmichal.filesystemmonitor.FilesystemEventType.INITIAL;
import static pl.mielecmichal.filesystemmonitor.FilesystemEventType.MODIFIED;

@Slf4j
class RecursivePathTest {

	@ParameterizedTest
	@EnumSource(ModificationKind.class)
	void shouldWatchModifiedFile(ModificationKind modificationKind, @TempDir Path temporaryDirectory) throws InterruptedException {
		//given
		Path recursive = Filesystem.createDirectory(temporaryDirectory, "recursive");
		Path recursiveFile = Filesystem.createFile(recursive, "recursive.txt");

		//when
		CountDownLatch countDownLatch = new CountDownLatch(3);
		List<FilesystemEvent> receivedEvents = new ArrayList<>();
		FilesystemNotifier monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryDirectory)
				.watchedConstraints(FilesystemConstraints.DEFAULT.withRecursive(true))
				.watchedConsumer(event -> {
					try {
						log.info("permissions: {}", Files.getPosixFilePermissions(event.getPath()));
					} catch (IOException e) {
						e.printStackTrace();
					}

					receivedEvents.add(event);
					countDownLatch.countDown();
				})
				.build();

		//when
		monitor.startWatching();
		Thread.sleep(100);
		modificationKind.accept(recursiveFile);

		countDownLatch.await(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

		//then
		Assertions.assertThat(receivedEvents).contains(
				FilesystemEvent.builder().path(recursive).eventType(INITIAL).build(),
				FilesystemEvent.builder().path(recursiveFile).eventType(INITIAL).build(),
				FilesystemEvent.builder().path(recursiveFile).eventType(MODIFIED).build()
		);
	}
}
