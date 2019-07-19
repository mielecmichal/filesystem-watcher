package pl.mielecmichal.filesystemmonitor.utilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;

import java.util.Collection;

public class AwaitilityUtils {

    private static final Duration DEFAULT_DURATION = Duration.TWO_HUNDRED_MILLISECONDS;

    public static void awaitForSize(Collection collection, int expectedSize) {
        awaitForSize(collection, expectedSize, DEFAULT_DURATION);
    }

    public static void awaitForSize(Collection collection, int expectedSize, Duration timeout) {
        Awaitility.await().atMost(timeout).until(() -> collection.size() == expectedSize);
    }
}
