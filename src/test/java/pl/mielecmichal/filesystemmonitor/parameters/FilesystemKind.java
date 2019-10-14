package pl.mielecmichal.filesystemmonitor.parameters;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mielecmichal.filesystemmonitor.utilities.FilesystemUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

@RequiredArgsConstructor
public enum FilesystemKind {
    CURRENT(FilesystemKind::createCurrent),
    JIMFS_UNIX(FilesystemKind::createJimfsUnix),
    JIMFS_WINDOWS(FilesystemKind::createJimfsWindows),
    JIMFS_OS_X(FilesystemKind::createJimfsOsX);

    @Getter
    private final Supplier<Path> pathSupplier;

    private static Path createCurrent() {
        try {
            return Files.createTempDirectory(generateDirectoryName());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path createJimfsUnix() {
        return createJimfs(Configuration.unix().toBuilder()
                .setAttributeViews("basic", "posix")
                .build());
    }

    private static Path createJimfsWindows() {
        return createJimfs(Configuration.windows().toBuilder()
                .setAttributeViews("basic", "dos")
                .build());
    }

    private static Path createJimfsOsX() {
        return createJimfs(Configuration.osX().toBuilder()
                .setAttributeViews("basic", "posix")
                .build());
    }

    private static Path createJimfs(Configuration configuration) {
        FileSystem jimfs = Jimfs.newFileSystem(configuration);
        return FilesystemUtils.createDirectory(jimfs.getPath(generateDirectoryName()));
    }

    private static String generateDirectoryName() {
        return "test" + UUID.randomUUID().toString();
    }

}
