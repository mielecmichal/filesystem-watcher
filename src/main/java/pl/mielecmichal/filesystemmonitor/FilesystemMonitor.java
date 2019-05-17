package pl.mielecmichal.filesystemmonitor;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

@Value
@Builder
public class FilesystemMonitor implements FilesystemWatcher {

	private final Path watchedPath;
	private final Consumer<FilesystemEvent> watchedConsumer;
	private final FilesystemConstraints watchedConstraints = new FilesystemConstraints();
	private final LinkedHashSet<FilesystemEvent> readerBuffer = new LinkedHashSet<>();

	@NonFinal
	private FilesystemWatcher watcher;
	@NonFinal
	private FilesystemWatcher reader;
	@NonFinal
	private boolean readerCompleted = false;

	@Override
	public void startWatching() {

		watcher = RecursiveFilesystemMonitor.builder()
				.watchedPath(watchedPath)
				.watchedConstraints(watchedConstraints)
				.watchedConsumer(this::consumeEvent)
				.build();

		reader = NioFilesystemReader.builder()
				.watchedPath(watchedPath)
				.watchedConstraints(watchedConstraints)
				.watchedConsumer(this::consumeEvent)
				.build();

		watcher.startWatching();
		reader.startWatching();
		readerCompleted = true;
		readerBuffer.forEach(watchedConsumer);
	}

	@Override
	public void stopWatching() {
		reader.stopWatching();
		watcher.stopWatching();
	}

	private void consumeEvent(FilesystemEvent event) {
		if (!readerCompleted) {
			readerBuffer.add(event);
			return;
		}
		watchedConsumer.accept(event);
	}
}
