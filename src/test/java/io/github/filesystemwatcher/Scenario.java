package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.WatchCoordinator;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

interface Scenario extends BiFunction<Path, WatchCoordinator, List<FilesystemEvent>> {
}
