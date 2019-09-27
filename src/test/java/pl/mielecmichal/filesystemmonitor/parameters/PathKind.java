package pl.mielecmichal.filesystemmonitor.parameters;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
public enum PathKind implements Function<Path, PathKind.PathScenario> {
    DIRECTORY(path -> {
        Path directory = FilesystemUtils.createDirectory(path, "directory");
        return PathScenario.of(directory);
    }),
    FILE(path -> {
        Path file = FilesystemUtils.createFile(path, "test.txt");
        return PathScenario.of(file);
    }),
    FILE_SYMLINK(path -> {
        Path filePath = FILE.apply(path).getSubjectPath();
        Path linkPath = FilesystemUtils.createLink(filePath, "symlink", path);
        return PathScenario.of(linkPath, filePath, List.of(linkPath, filePath));
    }),
    RECURSIVE_DIRECTORY(path -> {
        Path first = FilesystemUtils.createDirectory(path, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        return PathScenario.of(third, List.of(first, second, third));
    }),
    RECURSIVE_FILE(path -> {
        Path first = FilesystemUtils.createDirectory(path, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createFile(second, "recursive.txt");
        return PathScenario.of(third, List.of(first, second, third));
    });

    private final Function<Path, PathScenario> allPathsSupplier;

    @Override
    public PathScenario apply(Path path) {
        return allPathsSupplier.apply(path);
    }

    @Getter
    @RequiredArgsConstructor
    public static class PathScenario {
        private final Path testedPath;
        private final Path subjectPath;
        private final List<Path> allPaths;

        static PathScenario of(Path tested) {
            return of(tested, List.of(tested));
        }

        static PathScenario of(Path tested, List<Path> all) {
            return of(tested, tested, all);
        }

        static PathScenario of(Path tested, Path subject, List<Path> all) {
            return new PathScenario(tested, subject, all);
        }
    }
}
