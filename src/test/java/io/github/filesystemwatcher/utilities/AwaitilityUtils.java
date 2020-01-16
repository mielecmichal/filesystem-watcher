package io.github.filesystemwatcher.utilities;

import lombok.experimental.UtilityClass;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

@UtilityClass
public class AwaitilityUtils {

    private static final Duration DEFAULT_WAIT_TIME = Duration.ofSeconds(1);

	public static void awaitForSize(Collection collection, int expectedSize) {
		Awaitility.await()
				.atMost(DEFAULT_WAIT_TIME)
				.until(() -> collection.size() == expectedSize);
	}

	public static void awaitForSize(Map map, int expectedSize) {
		Awaitility.await()
				.atMost(DEFAULT_WAIT_TIME)
				.until(() -> map.size() == expectedSize);
	}
}
