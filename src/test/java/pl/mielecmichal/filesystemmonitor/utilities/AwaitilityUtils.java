package pl.mielecmichal.filesystemmonitor.utilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;

import java.util.Collection;

public class AwaitilityUtils {

	private static final Duration DEFAULT_WAIT_TIME = Duration.ONE_HUNDRED_MILLISECONDS;

	public static void awaitForSize(Collection collection, int expectedSize) {
		Awaitility.await()
				.atLeast(DEFAULT_WAIT_TIME)
				.until(() -> collection.size() == expectedSize);
	}
}
