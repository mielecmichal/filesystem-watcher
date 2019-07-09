package pl.mielecmichal.filesystemmonitor;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Builder
@Value
public class FilesystemReader implements FilesystemNotifier {

    private final Path watchedPath;
    private final FilesystemConstraints watchedConstraints;
    private final Consumer<FilesystemEvent> watchedConsumer;

    @Override
    public void startWatching() {
        ConstraintsFilteringVisitor visitor = new ConstraintsFilteringVisitor(watchedPath, watchedConstraints);
        try {
            Files.walkFileTree(watchedPath, visitor);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        visitor.getEvents().forEach(watchedConsumer);
    }

    @Override
    public void stopWatching() {
        // For now we do not need to implement this. Maybe later to make this class more responsive to stop.
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static final class ConstraintsFilteringVisitor extends SimpleFileVisitor<Path> {

        private final Path watchedPatch;
        private final List<FilesystemEvent> events = new ArrayList<>();
        private final FilesystemConstraints constraints;

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            super.postVisitDirectory(dir, exc);

            if (dir.equals(watchedPatch)) {
                return FileVisitResult.CONTINUE;
            }

            addFilesystemEvent(dir);

            if (!constraints.isRecursive()) {
                return FileVisitResult.TERMINATE;
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            super.visitFile(file, attrs);
            addFilesystemEvent(file);
            return FileVisitResult.CONTINUE;
        }

        private void addFilesystemEvent(Path path) {
            FilesystemEvent filesystemEvent = FilesystemEvent.builder()
                    .eventType(FilesystemEventType.INITIAL)
                    .path(path)
                    .build();

            if (constraints.test(filesystemEvent)) {
                events.add(filesystemEvent);
            }
        }
    }
}
