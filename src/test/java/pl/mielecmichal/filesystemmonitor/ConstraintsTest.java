package pl.mielecmichal.filesystemmonitor;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ConstraintsTest {

	private static class ConstraintTestSetup {

		final List<Path> all;
		private final Path firstDirectory;
		private final Path secondDirectory;
		private final Path thirdDirectory;
		private final Path firstFile;
		private final Path secondFile;
		private final Path thirdFile;
		private final Path recursive;
		private final Path firstRecursiveDirectory;
		private final Path secondRecursiveDirectory;
		private final Path firstRecursiveFile;
		private final Path secondRecursiveFile;

		ConstraintTestSetup(Path temporaryDirectory) {
			firstDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "first_directory");
			secondDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "second_directory");
			thirdDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "third_directory");
			firstFile = FilesystemUtils.createFile(temporaryDirectory, "firstFile.txt");
			secondFile = FilesystemUtils.createFile(temporaryDirectory, "secondFile.txt");
			thirdFile = FilesystemUtils.createFile(temporaryDirectory, "thirdFile.txt");

			recursive = FilesystemUtils.createDirectory(temporaryDirectory, "recursive_directory");
			firstRecursiveDirectory = FilesystemUtils.createDirectory(recursive, "first_recursive_directory");
			secondRecursiveDirectory = FilesystemUtils.createDirectory(recursive, "second_recursive_directory");

			firstRecursiveFile = FilesystemUtils.createFile(recursive, "firstFile.txt");
			secondRecursiveFile = FilesystemUtils.createFile(recursive, "secondFile.txt");

			this.all = List.of(firstDirectory,
					secondDirectory,
					thirdDirectory,
					firstFile,
					secondFile,
					thirdFile,
					recursive,
					firstRecursiveFile,
					secondRecursiveFile,
					firstRecursiveDirectory,
					secondRecursiveDirectory);
		}
	}


	@Test
	void shouldFindProperFilesForCorrectConstraints(@TempDir Path temporaryDirectory) throws InterruptedException {
		ConstraintTestSetup setup = new ConstraintTestSetup(temporaryDirectory);
		List<FilesystemEvent> receivedEvents = new ArrayList<>();

		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryDirectory)
				.watchedConsumer(receivedEvents::add)
				.watchedConstraints(FilesystemConstraints.DEFAULT)
				.build();

		monitor.startWatching();
		Thread.sleep(2000);

		List<FilesystemEvent> notRecursive = Stream.of(
				setup.firstFile, setup.secondFile, setup.thirdFile,
				setup.firstDirectory, setup.secondDirectory, setup.thirdDirectory, setup.recursive)
				.map(path -> FilesystemEvent.of(path, FilesystemEventType.INITIAL))
				.collect(Collectors.toList());
		Assertions.assertThat(receivedEvents).containsExactlyInAnyOrderElementsOf(notRecursive);
	}
}
