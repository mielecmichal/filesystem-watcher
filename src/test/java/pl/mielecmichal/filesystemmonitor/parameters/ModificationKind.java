package pl.mielecmichal.filesystemmonitor.parameters;

import lombok.RequiredArgsConstructor;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor
public enum ModificationKind implements Function<Path, Path> {

    POSIX_ADD_PERMISSION(ModificationKind::addPermission),
    POSIX_REMOVE_PERMISSION(ModificationKind::removePermission),
    POSIX_SET_SAME_PERMISSIONS(ModificationKind::setSamePermissions),
    DOS_SET_ATTRIBUTE(ModificationKind::setAttribute);

    private final Function<Path, Path> fileModifier;


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

    private static Path setAttribute(Path path) {
        DosFileAttributeView dosView = Files.getFileAttributeView(path, DosFileAttributeView.class);
        FilesystemUtils.TryIO.with(() -> dosView.setArchive(true));
        return path;
    }

}
