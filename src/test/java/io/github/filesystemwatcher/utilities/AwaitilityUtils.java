package io.github.filesystemwatcher.utilities;

import com.sun.nio.file.SensitivityWatchEventModifier;
import lombok.experimental.UtilityClass;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

@UtilityClass
public class AwaitilityUtils {

    private static final Duration DEFAULT_WAIT_TIME = Duration.ofSeconds(2 * SensitivityWatchEventModifier.HIGH.sensitivityValueInSeconds());

    public static <T> void awaitForSize(Collection<T> collection, int expectedSize) {
        Awaitility.await()
                .atMost(DEFAULT_WAIT_TIME)
                .until(() -> collection.size() == expectedSize);
    }

    public static <K, V> void awaitForSize(Map<K, V> map, int expectedSize) {
        Awaitility.await()
                .atMost(DEFAULT_WAIT_TIME)
                .until(() -> map.size() == expectedSize);
    }
}
