package io.github.filesystemwatcher.utilities;

import com.sun.nio.file.SensitivityWatchEventModifier;
import lombok.experimental.UtilityClass;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.condition.OS;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

@UtilityClass
public class AwaitilityUtils {

    private static final Duration DEFAULT_WAIT_TIME = findWaitingDuration();

    public static <T> void awaitForSize(Collection<T> collection, int expectedSize) {
        Awaitility.await()
                .atMost(DEFAULT_WAIT_TIME)
                .until(() -> collection.size() >= expectedSize);
    }

    private static Duration findWaitingDuration() {
        int poolingFrequencyInSeconds = SensitivityWatchEventModifier.HIGH.sensitivityValueInSeconds();
        Duration defaultWaitTimeForPooling = Duration.ofSeconds(5 * poolingFrequencyInSeconds);
        Duration defaultWaitTimeForNotify = Duration.ofMillis(500);

        if (OS.MAC.isCurrentOs()) {
            return defaultWaitTimeForPooling;
        }

        return defaultWaitTimeForNotify;
    }
}
