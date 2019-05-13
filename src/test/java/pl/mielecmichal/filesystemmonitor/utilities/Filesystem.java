package pl.mielecmichal.filesystemmonitor.utilities;

import lombok.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

@Value
public class Filesystem {

	private final Path temporaryFolder;

	public static void deleteFile(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path createDirectory(Path path, String name) {
		try {
			return Files.createDirectory(path.resolve(name));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path createFile(Path path, String name) {
		try {
			return Files.createFile(path.resolve(name));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Set<PosixFilePermission> getPosixFilePermissions(Path path) {
		try {
			return Files.getPosixFilePermissions(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> permissions) {
		try {
			return Files.setPosixFilePermissions(path, permissions);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
