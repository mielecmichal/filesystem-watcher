package pl.mielecmichal.filewatcher;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class FilesystemMonitorTest {

    @TempDir
    Path temporaryFolder;

    @Test
    void shouldReadEmptyDirectory() throws IOException {
        //given
        List<FilesystemEvent> receivedEvents = new ArrayList<>();

        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConsumer(receivedEvents::add)
                .build();

        //when
        monitor.watch();

        //then
        Assertions.assertThat(receivedEvents).hasSize(0);
    }

    @Test
    void shouldReadOneInitialFile() throws IOException {
        //given
        Path testFile = temporaryFolder.resolve("test.txt");
        Files.createFile(testFile);
        List<FilesystemEvent> receivedEvents = new ArrayList<>();

        FilesystemMonitor monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryFolder)
                .watchedConsumer(receivedEvents::add)
                .build();

        //when
        monitor.watch();

        //then
        Assertions.assertThat(receivedEvents).hasSize(1);
    }

}