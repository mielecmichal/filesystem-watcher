package pl.mielecmichal.filesystemmonitor;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ConstraintsTest {

	private static Path temporaryDirectory;

	@BeforeAll
	static void setUp(@TempDir Path tempDirectory) {
		temporaryDirectory = tempDirectory;
	}

	private static class ConstraintTestSetup {
		private final Path firstDirectory;
		private final Path secondDirectory;
		private final Path thirdDirectory;
		private final Path firstFile;
		private final Path secondFile;
		private final Path thirdFile;
		private final Path link;

		private final Path recursive;
		private final Path firstRecursiveDirectory;
		private final Path secondRecursiveDirectory;
		private final Path firstRecursiveFile;
		private final Path secondRecursiveFile;
		private final Path recursiveLink;

		ConstraintTestSetup(Path temporaryDirectory) {
			this.firstDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "first_directory");
			this.secondDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "second_directory");
			this.thirdDirectory = FilesystemUtils.createDirectory(temporaryDirectory, "third_directory");
			this.firstFile = FilesystemUtils.createFile(temporaryDirectory, "firstFile.txt");
			this.secondFile = FilesystemUtils.createFile(temporaryDirectory, "secondFile.xls");
			this.thirdFile = FilesystemUtils.createFile(temporaryDirectory, "thirdFile.txt");
			this.link = FilesystemUtils.createLink(thirdDirectory, "link", temporaryDirectory);

			this.recursive = FilesystemUtils.createDirectory(temporaryDirectory, "recursive_directory");
			this.firstRecursiveDirectory = FilesystemUtils.createDirectory(recursive, "first_recursive_directory");
			this.secondRecursiveDirectory = FilesystemUtils.createDirectory(recursive, "second_recursive_directory");
			this.firstRecursiveFile = FilesystemUtils.createFile(recursive, "firstFile.txt");
			this.secondRecursiveFile = FilesystemUtils.createFile(recursive, "secondFile.xls");
			this.recursiveLink = FilesystemUtils.createLink(secondRecursiveDirectory, "recursiveLink", recursive);
		}

		List<Path> getAll() {
			return List.of(firstDirectory,
					secondDirectory,
					thirdDirectory,
					firstFile,
					secondFile,
					thirdFile,
					link,
					recursive,
					firstRecursiveFile,
					secondRecursiveFile,
					firstRecursiveDirectory,
					secondRecursiveDirectory,
					recursiveLink
			);
		}

		List<Path> getAllNotRecursive() {
			return List.of(
					firstFile, secondFile, thirdFile, firstDirectory, secondDirectory, thirdDirectory, recursive, link
			);
		}
	}

	@ParameterizedTest
	@MethodSource
	void shouldFindProperFilesForCorrectConstraints(FilesystemConstraints constraints, List<Path> expectedPaths) throws InterruptedException {
		List<FilesystemEvent> receivedEvents = new ArrayList<>();

		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryDirectory)
				.watchedConsumer(receivedEvents::add)
				.watchedConstraints(constraints)
				.build();

		monitor.startWatching();
		Thread.sleep(100);

		List<FilesystemEvent> notRecursive = expectedPaths.stream()
				.map(path -> FilesystemEvent.of(path, FilesystemEventType.INITIAL))
				.collect(Collectors.toList());
		Assertions.assertThat(receivedEvents).containsExactlyInAnyOrderElementsOf(notRecursive);
	}

	private static Stream<Arguments> shouldFindProperFilesForCorrectConstraints() {
		ConstraintTestSetup setup = new ConstraintTestSetup(temporaryDirectory);

		return Stream.of(
				Arguments.of(
						FilesystemConstraints.DEFAULT,
						setup.getAllNotRecursive()
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withRecursive(true),
						setup.getAll()
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withFilenameSubstrings(List.of("first")),
						List.of(setup.firstFile, setup.firstDirectory)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withFilenamePatterns(List.of(Pattern.compile("^first.*"))),
						List.of(setup.firstFile, setup.firstDirectory)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withRecursive(true).withFilenameSubstrings(List.of("first")),
						List.of(setup.firstFile, setup.firstDirectory, setup.firstRecursiveFile, setup.firstRecursiveDirectory)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withRecursive(true).withFilenamePatterns(List.of(Pattern.compile("^first.*"))),
						List.of(setup.firstFile, setup.firstDirectory, setup.firstRecursiveFile, setup.firstRecursiveDirectory)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withFileTypes(List.of(FilesystemConstraints.FileType.DIRECTORY)),
						List.of(setup.firstDirectory, setup.secondDirectory, setup.thirdDirectory, setup.recursive)
				),
				Arguments.of(
						FilesystemConstraints.DEFAULT.withRecursive(true).withFileTypes(List.of(FilesystemConstraints.FileType.DIRECTORY)),
						List.of(setup.firstDirectory, setup.secondDirectory, setup.thirdDirectory, setup.recursive, setup.firstRecursiveDirectory, setup.secondRecursiveDirectory)
				)
		);
	}


}
