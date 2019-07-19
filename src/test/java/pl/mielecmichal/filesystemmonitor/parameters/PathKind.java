package pl.mielecmichal.filesystemmonitor.parameters;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
public enum PathKind implements Function<Path, List<Path>> {
	DIRECTORY(path -> List.of(FilesystemUtils.createDirectory(path, "directory")), 1),
	FILE(path -> List.of(FilesystemUtils.createFile(path, "test.txt")), 1),
	RECURSIVE_DIRECTORY(path -> {
		Path first = FilesystemUtils.createDirectory(path, "first");
		Path second = FilesystemUtils.createDirectory(first, "second");
		Path third = FilesystemUtils.createDirectory(second, "third");
		return List.of(first, second, third);
	}, 3),
	RECURSIVE_FILE(path -> {
		Path first = FilesystemUtils.createDirectory(path, "first");
		Path second = FilesystemUtils.createDirectory(first, "second");
		Path third = FilesystemUtils.createFile(second, "recursive.txt");
		return List.of(first, second, third);
	}, 3);

	private final Function<Path, List<Path>> pathsSupplier;
	@Getter
	private final int numberOfPaths;

	@Override
	public List<Path> apply(Path path) {
		return pathsSupplier.apply(path);
	}
}
