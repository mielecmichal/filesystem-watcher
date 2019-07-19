package pl.mielecmichal.filesystemmonitor.parameters;

import lombok.RequiredArgsConstructor;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
public enum ModificationStrategy implements Consumer<Path> {

	ADD_POSIX_PERMISSION(ModificationStrategy::addPermission),
	REMOVE_POSIX_PERMISSION(ModificationStrategy::removePermission),
	SET_SAME_PERMISSIONS(ModificationStrategy::setSamePermissions);

	private final Consumer<Path> fileModifier;

	@Override
	public void accept(Path path) {
		fileModifier.accept(path);
	}

	private static void addPermission(Path path) {
		Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(path);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		FilesystemUtils.setPosixFilePermissions(path, permissions);
	}

	private static void removePermission(Path path) {
		Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(path);
		PosixFilePermission toRemove = permissions.stream()
				.filter(permission -> !permission.toString().startsWith("OWNER"))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", path)));
		permissions.remove(toRemove);
		FilesystemUtils.setPosixFilePermissions(path, permissions);
	}

	private static void setSamePermissions(Path path) {
		Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(path);
		FilesystemUtils.setPosixFilePermissions(path, permissions);
	}

}
