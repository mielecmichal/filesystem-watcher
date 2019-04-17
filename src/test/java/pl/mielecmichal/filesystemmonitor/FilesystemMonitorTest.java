package pl.mielecmichal.filesystemmonitor;


import org.assertj.core.api.Assertions;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static pl.mielecmichal.filesystemmonitor.FilesystemEvent.FilesystemEventType.*;

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
	void shouldReadInitialFile() {
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
		monitor.watch();
		Path testFile = createFile("test.txt");
		countDownLatch.await(200, TimeUnit.MILLISECONDS);

		//then
		Assertions.assertThat(receivedEvents).hasSize(1);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath).containsExactly(testFile);
	}

	@Test
	void shouldWatchDeletedFile() throws InterruptedException {
		//given
		Path testFile = createFile("test.txt");

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
		monitor.watch();
		deleteFile(testFile);
		latch.await(2000, TimeUnit.MILLISECONDS);

		//then
		Assertions.assertThat(receivedEvents).hasSize(2);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath)
				.contains(testFile, testFile);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getEventType)
				.contains(INITIAL, DELETED);

	}

	@Test
	void shouldWatchModifiedFile() throws InterruptedException, IOException {
		//given
		Path testFile = createFile("test.txt");

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
		monitor.watch();
		Files.setPosixFilePermissions(testFile, Sets.newLinkedHashSet(PosixFilePermission.OTHERS_READ));
		latch.await(2000, TimeUnit.MILLISECONDS);

		//then
		Assertions.assertThat(receivedEvents).hasSize(2);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getPath)
				.contains(testFile, testFile);
		Assertions.assertThat(receivedEvents).extracting(FilesystemEvent::getEventType)
				.contains(INITIAL, MODIFIED);

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

	private void deleteFile(Path filePath) {
		Path testFile = temporaryFolder.resolve(filePath);
		try {
			Files.deleteIfExists(testFile);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}