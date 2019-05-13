package pl.mielecmichal.filesystemmonitor;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static pl.mielecmichal.filesystemmonitor.FilesystemEvent.FilesystemEventType.*;

@Log
@Builder
@RequiredArgsConstructor
public class RecursiveFilesystemMonitor implements FilesystemWatcher {

	private final Path watchedPath;
	private final FilesystemConstraints watchedConstraints;
	private final Consumer<FilesystemEvent> watchedConsumer;

	@Builder.Default
	private final ExecutorService producersExecutor = Executors.newSingleThreadExecutor();
	@Builder.Default
	private final ExecutorService consumersExecutor = Executors.newSingleThreadExecutor();

	private final Map<Path, FilesystemWatcher> watchers = new ConcurrentHashMap<>();

	@Override
	public void startWatching() {
		startRecursiveWatcher(watchedPath);
	}

	@Override
	public void stopWatching() {
		watchers.values().forEach(FilesystemWatcher::stopWatching);
	}

	private void consumeEventInternally(FilesystemEvent event){
		Path path = event.getPath();
		if(!path.equals(watchedPath) && Files.isDirectory(path)) {
			handleRecursiveDirectory(event);
		}
		watchedConsumer.accept(event);
	}

	private void handleRecursiveDirectory(FilesystemEvent event){
		if(event.getEventType() == INITIAL || event.getEventType() == CREATED){
			startRecursiveWatcher(event.getPath());
		} else if(event.getEventType() == DELETED){
			stopRecursiveWatcher(event.getPath());
		}
	}

	private void startRecursiveWatcher(Path path){
		FilesystemWatcher pathWatcher = createDirectoryWatcher(path);
		pathWatcher.startWatching();
		watchers.put(path, pathWatcher);
	}

	private void stopRecursiveWatcher(Path path){
		FilesystemWatcher watcher = watchers.get(path);
		watcher.stopWatching();
		watchers.remove(path);
	}

	private FilesystemWatcher createDirectoryWatcher(Path path){
		return NioFilesystemWatcher.builder()
				.watchedConstraints(watchedConstraints)
				.watchedConsumer(this::consumeEventInternally)
				.watchedPath(path)
				.producersExecutor(producersExecutor)
				.consumersExecutor(consumersExecutor)
				.build();
	}

}
