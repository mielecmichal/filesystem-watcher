package pl.mielecmichal.filesystemmonitor;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;

@Value
@Builder
public class FilesystemEvent {
    private final Path path;
    private final FilesystemEventType eventType;

    public static FilesystemEvent of(FilesystemEventType eventType, Path path) {
        return FilesystemEvent.builder()
                .eventType(eventType)
                .path(path)
                .build();
    }

    static FilesystemEvent of(WatchEvent watchedEvent, Path watchedDirectoryPath) {
        Path watchedElement = (Path) watchedEvent.context();
        Path totalPath = Paths.get(watchedDirectoryPath.toString(), watchedElement.toString());
        return FilesystemEvent.builder()
                .eventType(FilesystemEventType.of(watchedEvent.kind()))
                .path(totalPath)
                .build();
    }
}
