package pl.mielecmichal.filesystemmonitor;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Value
@Builder
public class FilesystemMonitor implements FilesystemWatcher {

	private final Path watchedPath;
	private final Consumer<FilesystemEvent> watchedConsumer;
	private final FilesystemConstraints watchedConstraints = FilesystemConstraints.DEFAULT;
	private final LinkedHashSet<FilesystemEvent> readerBuffer = new LinkedHashSet<>();

	private final ExecutorService producersExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "filesystem-monitor-producers" + UUID.randomUUID().toString()));
	private final ExecutorService consumersExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "filesystem-monitor-consumers" + UUID.randomUUID().toString()));

	@NonFinal
	private FilesystemWatcher watcher;
	@NonFinal
	private FilesystemWatcher reader;
	@NonFinal
	private boolean readerCompleted = false;

	@Override
	public void startWatching() {

		if(watchedConstraints.isRecursive()) {
			watcher = RecursiveFilesystemMonitor.builder()
					.watchedPath(watchedPath)
					.watchedConstraints(watchedConstraints)
					.watchedConsumer(this::consumeEvent)
					.build();
		} else {
			watcher = NioFilesystemWatcher.builder()
					.watchedPath(watchedPath)
					.watchedConstraints(watchedConstraints)
					.watchedConsumer(this::consumeEvent)
					.producersExecutor(producersExecutor)
					.consumersExecutor(consumersExecutor)
					.build();
		}

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
