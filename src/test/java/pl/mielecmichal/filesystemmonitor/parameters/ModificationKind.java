package pl.mielecmichal.filesystemmonitor.parameters;

import lombok.RequiredArgsConstructor;
import pl.mielecmichal.filesystemmonitor.utilities.Filesystem;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
public enum ModificationKind implements Consumer<Path> {

	ADD_POSIX_PERMISSION(ModificationKind::addPermission),
	REMOVE_POSIX_PERMISSION(ModificationKind::removePermission),
	SET_SAME_PERMISSIONS(ModificationKind::setSamePermissions);

	private final Consumer<Path> fileModifier;

	@Override
	public void accept(Path path) {
		fileModifier.accept(path);
	}

	private static void addPermission(Path path) {
		Set<PosixFilePermission> permissions = Filesystem.getPosixFilePermissions(path);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		Filesystem.setPosixFilePermissions(path, permissions);
	}

	private static void removePermission(Path path) {
		Set<PosixFilePermission> permissions = Filesystem.getPosixFilePermissions(path);
		PosixFilePermission toRemove = permissions.stream()
				.filter(permission -> !permission.toString().startsWith("OWNER"))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", path)));
		permissions.remove(toRemove);
		Filesystem.setPosixFilePermissions(path, permissions);
	}

	private static void setSamePermissions(Path path) {
		Set<PosixFilePermission> permissions = Filesystem.getPosixFilePermissions(path);
		Filesystem.setPosixFilePermissions(path, permissions);
	}

}
