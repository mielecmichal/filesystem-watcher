package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.FilesystemUtils;
import io.github.filesystemwatcher.utilities.WatchCoordinator;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static io.github.filesystemwatcher.FilesystemEventType.*;

@RequiredArgsConstructor
enum SingleDirectoryScenario implements Scenario{

    DIRECTORY_CREATE(SingleDirectoryScenario::create),
    DIRECTORY_DELETE(SingleDirectoryScenario::delete),
    DIRECTORY_RENAME(SingleDirectoryScenario::rename),
    DIRECTORY_ADD_POSIX_PERMISSIONS(SingleDirectoryScenario::addPermissions),
    DIRECTORY_REMOVE_POSIX_PERMISSIONS(SingleDirectoryScenario::removePermissions),
    DIRECTORY_SET_SAME_PERMISSIONS(SingleDirectoryScenario::setSamePermissions);

    private final Scenario scenario;

    @Override
    public List<FilesystemEvent> apply(Path path, WatchCoordinator watchCoordinator) {
        return scenario.apply(path, watchCoordinator);
    }

    private static List<FilesystemEvent> create(Path base, WatchCoordinator coordinator) {
        coordinator.setupCompleted();

        Path created = FilesystemUtils.createDirectory(base, "test");
        return List.of(FilesystemEvent.of(created, CREATED));
    }

    private static List<FilesystemEvent> delete(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");

        coordinator.setupCompleted();

        FilesystemUtils.delete(created);
        return List.of(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, DELETED)
        );
    }

    private static List<FilesystemEvent> rename(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");
        coordinator.setupCompleted();

        Path moved = FilesystemUtils.move(created, base.resolve("test2"));

        return List.of(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, DELETED),
                FilesystemEvent.of(moved, CREATED)
        );
    }

    private static List<FilesystemEvent> addPermissions(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(created);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        FilesystemUtils.setPosixFilePermissions(created, permissions);
        return List.of(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }

    private static List<FilesystemEvent> removePermissions(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(created);
        PosixFilePermission toRemove = permissions.stream()
                .filter(permission -> !permission.toString().startsWith("OWNER"))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", created)));
        permissions.remove(toRemove);
        FilesystemUtils.setPosixFilePermissions(created, permissions);
        return List.of(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }

    private static List<FilesystemEvent> setSamePermissions(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(created);
        FilesystemUtils.setPosixFilePermissions(created, permissions);

        return List.of(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }
}
