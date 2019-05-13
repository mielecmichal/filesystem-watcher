package pl.mielecmichal.filesystemmonitor;

import com.sun.nio.file.ExtendedWatchEventModifier;
import io.vavr.control.Try;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;

import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

@Log
@Builder
@Value
public class NioFilesystemWatcher implements FilesystemWatcher{

    private static final WatchEvent.Kind[] ALL_WATCH_KINDS = new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW};

    private final Path watchedPath;
    private final FilesystemConstraints watchedConstraints;
    private final Consumer<FilesystemEvent> watchedConsumer;

    private final BlockingQueue<FilesystemEvent> blockingQueue = new ArrayBlockingQueue<>(100000);
    @Builder.Default
    private final ExecutorService producersExecutor = Executors.newSingleThreadExecutor();
    @Builder.Default
    private final ExecutorService consumersExecutor = Executors.newSingleThreadExecutor();

    private WatchService watcher = Try.of(() -> FileSystems.getDefault().newWatchService()).getOrElseThrow((e) -> new IllegalStateException(e));
    @NonFinal
	private Future<?> consumer;
	@NonFinal
	private Future<?> producer;
    @Override
    public void startWatching() {
        Try.of(() -> watchedPath.register(watcher, ALL_WATCH_KINDS)).getOrElseThrow((e) -> new IllegalStateException(e));
		 consumer = consumersExecutor.submit(this::consumeEvents);
		 producer = producersExecutor.submit(this::produceEvents);
	}

	@Override
	public void stopWatching() {
    	blockingQueue.clear();
		producersExecutor.shutdownNow();
		consumersExecutor.shutdownNow();
	}

	private void produceEvents() {
        try {
            WatchKey take = watcher.take();
            List<WatchEvent<?>> watchEvents = take.pollEvents();
            for (WatchEvent<?> watchEvent : watchEvents) {

            	if(Thread.currentThread().isInterrupted()){
            		break;
				}

                log.fine(String.format("Found startWatching event = %s", watchEvent));
                if (watchEvent.kind() == OVERFLOW) {
                    log.severe(String.format("OVERFLOW watchEvent occurred %s times. Operating system queue was overflowed. File events could be lost.", watchEvent.count()));
                    continue;
                }

                Path filePath = (Path) watchEvent.context();
				Path path = Paths.get(watchedPath.toString(), filePath.toString());

				FilesystemEvent filesystemEvent = FilesystemEvent.builder()
                        .eventType(FilesystemEvent.FilesystemEventType.of(watchEvent.kind()))
                        .path(path)
                        .build();


                if (watchedConstraints.test(filesystemEvent)) {
					log.info("Found " + filesystemEvent);
					blockingQueue.put(filesystemEvent);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e){
        	log.severe(e.getMessage());
		}
    }

    private void consumeEvents() {
        try {
            FilesystemEvent event = blockingQueue.take();
			log.info("Took " + event);

			watchedConsumer.accept(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


}
