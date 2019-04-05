package pl.mielecmichal.filewatcher;

import lombok.Builder;
import lombok.Value;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Log
@Builder
@Value
public class NioFilesystemReader implements FilesystemWatcher{

    private final Path watchedPath;
    private final FilesystemConstraints watchedConstraints;
    private final Consumer<FilesystemEvent> watchedConsumer;

    @Override
    public void watch() {
        ConstraintsFilteringVisitor visitor = new ConstraintsFilteringVisitor(watchedConstraints);
        try {
            Files.walkFileTree(watchedPath, visitor);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        visitor.getEvents().forEach(watchedConsumer::accept);
    }

    @Value
    private static final class ConstraintsFilteringVisitor implements FileVisitor<Path> {

        private final List<FilesystemEvent> events = new ArrayList<>();
        private final FilesystemConstraints constraints;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {

            FilesystemEvent filesystemEvent = FilesystemEvent.builder()
                    .eventType(FilesystemEvent.FilesystemEventType.INITIAL)
                    .path(path)
                    .build();

            if (constraints.test(filesystemEvent)) {
                events.add(filesystemEvent);
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return FileVisitResult.TERMINATE;
        }
    }
}
