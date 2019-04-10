package pl.mielecmichal.filewatcher;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

@Log
@Value
@Builder
public class FilesystemMonitor implements FilesystemWatcher {

    private final Path watchedPath;
    private final Consumer<FilesystemEvent> watchedConsumer;
    private final FilesystemConstraints watchedConstraints = new FilesystemConstraints();
    private final LinkedHashSet<FilesystemEvent> readerBuffer = new LinkedHashSet<>();

    @NonFinal
    private boolean readerCompleted;

    @Override
    public void watch() {

        NioFilesystemWatcher nioFilesystemWatcher = NioFilesystemWatcher.builder()
                .watchedPath(watchedPath)
                .watchedConstraints(watchedConstraints)
                .watchedConsumer(readerBuffer::add)
                .build();
        nioFilesystemWatcher.watch();

        NioFilesystemReader nioFilesystemReader = NioFilesystemReader.builder()
                .watchedPath(watchedPath)
                .watchedConstraints(watchedConstraints)
                .watchedConsumer(this::consumeEvent)
                .build();
        nioFilesystemReader.watch();
        readerCompleted = true;
        readerBuffer.forEach(watchedConsumer);
    }

    private void consumeEvent(FilesystemEvent event){
        if(!readerCompleted){
            readerBuffer.add(event);
            return;
        }
        watchedConsumer.accept(event);
    }
}
