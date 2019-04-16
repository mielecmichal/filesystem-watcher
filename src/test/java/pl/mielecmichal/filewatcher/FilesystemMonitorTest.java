package pl.mielecmichal.filewatcher;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class FilesystemMonitorTest {

	@TempDir
	Path temporaryFolder;

	@Test
	void shouldReadEmptyDirectory() {
		//given
		List<FilesystemEvent> receivedEvents = new ArrayList<>();

		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryFolder)
				.watchedConsumer(receivedEvents::add)
				.build();

		//when
		monitor.watch();

		//then
		Assertions.assertThat(receivedEvents).isEmpty();
	}

	@Test
	void shouldReadOneInitialFile() {
		//given
		Path testFile = createFile("test.txt");
		List<FilesystemEvent> receivedEvents = new ArrayList<>();
		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryFolder)
				.watchedConsumer(receivedEvents::add)
				.build();

		//when
		monitor.watch();

		//then
		Assertions.assertThat(receivedEvents).hasSize(1);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath).containsExactly(testFile);
	}

	@Test
	void shouldWatchOneFile() throws InterruptedException {
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
		monitor.watch();
		Path testFile = createFile("test.txt");
		countDownLatch.await(200, TimeUnit.MILLISECONDS);

		//then
		Assertions.assertThat(receivedEvents).hasSize(1);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath).containsExactly(testFile);
	}

	private Path createFile(String name) {
		Path testFile = temporaryFolder.resolve(name);
		try {
			Files.createFile(testFile);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return testFile;
	}
}