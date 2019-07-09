package pl.mielecmichal.filesystemmonitor.utilities;

import lombok.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Supplier;

@Value
public class Filesystem {

    private final Path temporaryFolder;

    public static void deleteFile(Path path) {
        TryIO.with(() -> Files.deleteIfExists(path)).get();
    }

    public static Path createDirectory(Path path, String name) {
        return TryIO.with(() -> Files.createDirectory(path.resolve(name))).get();
    }

    public static Path createFile(Path path, String name) {
        return TryIO.with(() -> Files.createFile(path.resolve(name))).get();
    }

    public static Set<PosixFilePermission> getPosixFilePermissions(Path path) {
        return TryIO.with(() -> Files.getPosixFilePermissions(path)).get();
    }

    public static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> permissions) {
        return TryIO.with(() -> Files.setPosixFilePermissions(path, permissions)).get();
    }

    static class TryIO {

        @FunctionalInterface
        interface ThrowingSupplier<T> {
            T apply() throws IOException;
        }

        static <T> Supplier<T> with(ThrowingSupplier<T> throwingSupplier) {
            return () -> {
                try {
                    return throwingSupplier.apply();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        }
    }
}
