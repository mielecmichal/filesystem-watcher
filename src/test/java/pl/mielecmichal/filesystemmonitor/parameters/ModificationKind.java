package pl.mielecmichal.filesystemmonitor.parameters;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mielecmichal.filesystemmonitor.FilesystemEventType;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor
public enum ModificationKind implements Function<Path, Path> {

	ADD_POSIX_PERMISSION(ModificationKind::addPermission, FilesystemEventType.MODIFIED),
	REMOVE_POSIX_PERMISSION(ModificationKind::removePermission, FilesystemEventType.MODIFIED),
    SET_SAME_PERMISSIONS(ModificationKind::setSamePermissions, FilesystemEventType.MODIFIED);

	private final Function<Path, Path> fileModifier;
	@Getter
	private final FilesystemEventType expectedEvent;

	@Override
	public Path apply(Path path) {
		return fileModifier.apply(path);
	}

	private static Path addPermission(Path path) {
		Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(path);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		return FilesystemUtils.setPosixFilePermissions(path, permissions);
	}

	private static Path removePermission(Path path) {
		Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(path);
		PosixFilePermission toRemove = permissions.stream()
				.filter(permission -> !permission.toString().startsWith("OWNER"))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", path)));
		permissions.remove(toRemove);
		return FilesystemUtils.setPosixFilePermissions(path, permissions);
	}

	private static Path setSamePermissions(Path path) {
		Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(path);
		return FilesystemUtils.setPosixFilePermissions(path, permissions);
	}

}
