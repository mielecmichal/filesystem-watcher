package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.WatchCoordinator;
import io.github.filesystemwatcher.utilities.WatchImplementation;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

interface Scenario extends BiFunction<Path, WatchCoordinator, List<FilesystemEvent>> {

    default List<WatchImplementation> getImplementations() {
        throw new IllegalStateException("Define applicable watch service implementations");
    }
}
