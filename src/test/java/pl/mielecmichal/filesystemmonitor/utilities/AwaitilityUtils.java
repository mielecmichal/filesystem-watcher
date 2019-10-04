package pl.mielecmichal.filesystemmonitor.utilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;

import java.util.Collection;
import java.util.Map;

public class AwaitilityUtils {

	private static final Duration DEFAULT_WAIT_TIME = Duration.FIVE_HUNDRED_MILLISECONDS;

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
