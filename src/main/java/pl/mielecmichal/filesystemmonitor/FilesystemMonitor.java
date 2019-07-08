package pl.mielecmichal.filesystemmonitor;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Value
@Builder
@Wither
public class FilesystemMonitor implements FilesystemNotifier {

	private final Path watchedPath;
	private final Consumer<FilesystemEvent> watchedConsumer;
	private final FilesystemConstraints watchedConstraints;

	private final BlockingQueue<FilesystemEvent> queue = new ArrayBlockingQueue<>(100000);
	private final ExecutorService producersExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "filesystem-monitor-producers" + UUID.randomUUID().toString()));
	private final ExecutorService consumersExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "filesystem-monitor-consumers" + UUID.randomUUID().toString()));

	@NonFinal
	private FilesystemNotifier watcher;
	@NonFinal
	private FilesystemNotifier reader;

	@Override
	public void startWatching() {
		watcher = FilesystemWatcher.builder()
				.watchedPath(watchedPath)
				.watchedConstraints(watchedConstraints)
				.watchedConsumer(watchedConsumer)
				.producersExecutor(producersExecutor)
				.consumersExecutor(consumersExecutor)
				.blockingQueue(queue)
				.build();

		reader = FilesystemReader.builder()
				.watchedPath(watchedPath)
				.watchedConstraints(watchedConstraints)
				.watchedConsumer(this::consumeEvent)
				.build();

		watcher.startWatching();
		reader.startWatching();
	}

	@Override
	public void stopWatching() {
		reader.stopWatching();
		watcher.stopWatching();
	}

	private void consumeEvent(FilesystemEvent event) {
		try {
			queue.put(event);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
