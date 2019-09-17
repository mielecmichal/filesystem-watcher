package pl.mielecmichal.filesystemmonitor;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class FilesystemConstraintsTest {

	private static class ConstraintTestSetuper {

		final List<Path> all;

		ConstraintTestSetuper(Path temporaryDirectory) {
			Path firstDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "first_directory");
			Path secondDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "second_directory");
			Path thirdDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "third_directory");
			Path firstFile = FilesystemUtils.createFile(temporaryDirectory, "firstFile.txt");
			Path secondFile = FilesystemUtils.createFile(temporaryDirectory, "secondFile.txt");
			Path thirdFile = FilesystemUtils.createFile(temporaryDirectory, "thirdFile.txt");

			Path recursive = FilesystemUtils.createDirectory(temporaryDirectory, "recursive_directory");
			Path firstRecursiveDirectory = FilesystemUtils.createDirectory(recursive, "first_recursive_directory");
			Path secondRecursiveDirectory = FilesystemUtils.createDirectory(recursive, "second_recursive_directory");

			Path firstRecursiveFile = FilesystemUtils.createFile(recursive, "firstFile.txt");
			Path secondRecursiveFile = FilesystemUtils.createFile(recursive, "secondFile.txt");

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
		ConstraintTestSetuper constraintTestSetuper = new ConstraintTestSetuper(temporaryDirectory);
		List<FilesystemEvent> receivedEvents = new ArrayList<>();

		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryDirectory)
				.watchedConsumer(receivedEvents::add)
				.watchedConstraints(FilesystemConstraints.DEFAULT)
				.build();

		monitor.startWatching();
		Thread.sleep(2000);

		List<FilesystemEvent> all = constraintTestSetuper.all.stream().map(path -> FilesystemEvent.of(path, FilesystemEventType.INITIAL)).collect(Collectors.toList());
		Assertions.assertThat(receivedEvents).containsAll(all);
	}
}
