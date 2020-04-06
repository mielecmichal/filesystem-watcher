package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.FilesystemUtils;
import io.github.filesystemwatcher.utilities.WatchCoordinator;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static io.github.filesystemwatcher.FilesystemEventType.*;
import static io.github.filesystemwatcher.FilesystemEventType.MODIFIED;

@RequiredArgsConstructor
enum RecursiveDirectoryScenario implements Scenario {

    RECURSIVE_DIRECTORY_CREATE(RecursiveDirectoryScenario::create),
    RECURSIVE_DIRECTORY_DELETE(RecursiveDirectoryScenario::delete),
    RECURSIVE_DIRECTORY_RENAME(RecursiveDirectoryScenario::rename),
    RECURSIVE_DIRECTORY_ADD_POSIX_PERMISSIONS(RecursiveDirectoryScenario::addPermissions),
    RECURSIVE_DIRECTORY_REMOVE_POSIX_PERMISSIONS(RecursiveDirectoryScenario::removePermissions),
    RECURSIVE_DIRECTORY_SET_SAME_PERMISSIONS(RecursiveDirectoryScenario::setSamePermissions);

    private final Scenario scenario;

    @Override
    public List<FilesystemEvent> apply(Path path, WatchCoordinator watchCoordinator) {
        return scenario.apply(path, watchCoordinator);
    }

    private static List<FilesystemEvent> create(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");

        coordinator.setupCompleted();

        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");

        return List.of(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, CREATED)
        );
    }

    private static List<FilesystemEvent> delete(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");

        coordinator.setupCompleted();

        FilesystemUtils.delete(recursiveDirectory);
        return List.of(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL),
                FilesystemEvent.of(recursiveDirectory, DELETED)
        );
    }

    //TODO move not empty dir?
    //TODO copy?
    private static List<FilesystemEvent> rename(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");

        coordinator.setupCompleted();

        Path moved = FilesystemUtils.move(recursiveDirectory, third.resolve("moved"));
        return List.of(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL),
                FilesystemEvent.of(recursiveDirectory, DELETED),
                FilesystemEvent.of(moved, CREATED)
        );
    }

    private static List<FilesystemEvent> addPermissions(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(recursiveDirectory);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        FilesystemUtils.setPosixFilePermissions(recursiveDirectory, permissions);
        return List.of(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL),
                FilesystemEvent.of(recursiveDirectory, MODIFIED)
        );
    }

    private static List<FilesystemEvent> removePermissions(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(recursiveDirectory);
        PosixFilePermission toRemove = permissions.stream()
                .filter(permission -> !permission.toString().startsWith("OWNER"))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", recursiveDirectory)));
        permissions.remove(toRemove);
        FilesystemUtils.setPosixFilePermissions(recursiveDirectory, permissions);
        return List.of(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL),
                FilesystemEvent.of(recursiveDirectory, MODIFIED)
        );
    }

    private static List<FilesystemEvent> setSamePermissions(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(recursiveDirectory);
        FilesystemUtils.setPosixFilePermissions(recursiveDirectory, permissions);

        return List.of(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL),
                FilesystemEvent.of(recursiveDirectory, MODIFIED)
        );
    }
}
