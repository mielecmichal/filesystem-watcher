package io.github.filesystemwatcher.utilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;

import java.util.Collection;
import java.util.Map;

public class AwaitilityUtils {

    private static final Duration DEFAULT_WAIT_TIME = Duration.ONE_SECOND;

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
