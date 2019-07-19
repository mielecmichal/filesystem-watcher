package pl.mielecmichal.filesystemmonitor;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class EmptyDirectoryTest {

	@Test
	void shouldNotEmitAnyEvents(@TempDir Path temporaryFolder) {
		//given
		List<FilesystemEvent> receivedEvents = new ArrayList<>();
		FilesystemMonitor monitor = FilesystemMonitor.builder()
				.watchedPath(temporaryFolder)
				.watchedConsumer(receivedEvents::add)
				.build();

		//when
		monitor.startWatching();

		//then
		Assertions.assertThat(receivedEvents).isEmpty();
	}

}
