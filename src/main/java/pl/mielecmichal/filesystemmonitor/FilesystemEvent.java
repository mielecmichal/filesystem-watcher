package pl.mielecmichal.filesystemmonitor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;

@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "of")
public class FilesystemEvent {
    private final Path path;
    private final FilesystemEventType eventType;

    static FilesystemEvent of(WatchEvent event, Path path) {
        Path watchedElement = (Path) event.context();
        Path totalPath = Paths.get(path.toString(), watchedElement.toString());
        return new FilesystemEvent(totalPath, FilesystemEventType.of(event.kind()));
    }
}
