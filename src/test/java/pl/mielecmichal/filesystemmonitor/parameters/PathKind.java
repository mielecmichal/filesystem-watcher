package pl.mielecmichal.filesystemmonitor.parameters;

import lombok.RequiredArgsConstructor;
import pl.mielecmichal.filesystemmonitor.utilities.Filesystem;

import java.nio.file.Path;
import java.util.function.Function;

@RequiredArgsConstructor
public enum PathKind implements Function<Path, Path> {
	DIRECTORY(path -> Filesystem.createDirectory(path, "directory")),
	FILE(path -> Filesystem.createFile(path, "test.txt"));

	private final Function<Path, Path> pathSupplier;

	@Override
	public Path apply(Path path) {
		return pathSupplier.apply(path);
	}
}
