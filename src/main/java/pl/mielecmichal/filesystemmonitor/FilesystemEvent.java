package pl.mielecmichal.filesystemmonitor;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

@Value
@Builder
public class FilesystemEvent {
    private final Path path;
    private final FilesystemEventType eventType;

    @RequiredArgsConstructor
    public enum FilesystemEventType {

        INITIAL,
        CREATED,
        DELETED,
        MODIFIED;

        static final Map<WatchEvent.Kind, FilesystemEventType> CORRESPONDING_WATCH_KINDS = Map.of(
                ENTRY_CREATE, CREATED,
                ENTRY_DELETE, DELETED,
                ENTRY_MODIFY, MODIFIED
        );

        static FilesystemEventType of(WatchEvent.Kind kind){
            return CORRESPONDING_WATCH_KINDS.get(kind);
        }
    }


}
