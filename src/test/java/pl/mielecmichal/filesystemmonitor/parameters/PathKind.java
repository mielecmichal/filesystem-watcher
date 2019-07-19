package pl.mielecmichal.filesystemmonitor.parameters;

import lombok.RequiredArgsConstructor;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Path;
import java.util.function.Function;

@RequiredArgsConstructor
public enum PathKind implements Function<Path, Path> {
    EMPTY_DIRECTORY(path -> FilesystemUtils.createDirectory(path, "directory")),
    FILE(path -> FilesystemUtils.createFile(path, "test.txt"));

	private final Function<Path, Path> pathSupplier;

	@Override
	public Path apply(Path path) {
		return pathSupplier.apply(path);
	}
}
