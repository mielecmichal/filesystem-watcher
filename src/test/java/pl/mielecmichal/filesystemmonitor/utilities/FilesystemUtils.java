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
public class FilesystemUtils {

    private final Path temporaryFolder;

    public static Path delete(Path path) {
        TryIO.with(() -> Files.deleteIfExists(path));
        return path;
    }

    public static Path createDirectory(Path path) {
        return TryIO.with(() -> Files.createDirectory(path));
    }

    public static Path createDirectory(Path path, String name) {
        return TryIO.with(() -> Files.createDirectory(path.resolve(name)));
    }

    public static Path createFile(Path path, String name) {
        return TryIO.with(() -> Files.createFile(path.resolve(name)));
    }

    public static Path createLink(Path linkTarget, String linkName, Path linkPlace) {
        return TryIO.with(() -> Files.createSymbolicLink(linkPlace.resolve(linkName), linkTarget));
    }

    public static Set<PosixFilePermission> getPosixFilePermissions(Path path) {
        return TryIO.with(() -> Files.getPosixFilePermissions(path));
    }

    public static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> permissions) {
        return TryIO.with(() -> Files.setPosixFilePermissions(path, permissions));
    }

    public static class TryIO {

        @FunctionalInterface
        public interface ThrowingSupplier<T> {
            T apply() throws IOException;
        }

        @FunctionalInterface
        public interface ThrowingRunnable<T> {
            void apply() throws IOException;
        }

        public static void with(ThrowingRunnable throwingSupplier) {
            Runnable runnable = () -> {
                try {
                    throwingSupplier.apply();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        }

        public static <T> T with(ThrowingSupplier<T> throwingSupplier) {
            Supplier<T> supplier = () -> {
                try {
                    return throwingSupplier.apply();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            return supplier.get();
        }
    }
}
