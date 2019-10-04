package io.github.filesystemwatcher;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Map;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum FilesystemEventType {
    INITIAL,
    CREATED,
    DELETED,
    MODIFIED;

    private static final Map<WatchEvent.Kind, FilesystemEventType> CORRESPONDING_WATCH_KINDS = Map.of(
            StandardWatchEventKinds.ENTRY_CREATE, FilesystemEventType.CREATED,
            StandardWatchEventKinds.ENTRY_DELETE, FilesystemEventType.DELETED,
            StandardWatchEventKinds.ENTRY_MODIFY, FilesystemEventType.MODIFIED
    );

    static FilesystemEventType of(WatchEvent.Kind kind) {
        return CORRESPONDING_WATCH_KINDS.get(kind);
    }
}
