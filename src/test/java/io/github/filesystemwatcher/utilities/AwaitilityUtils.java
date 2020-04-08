package io.github.filesystemwatcher.utilities;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.TimeoutEvent;

import java.time.Duration;
import java.util.Collection;

@Slf4j
@UtilityClass
public class AwaitilityUtils {

    private static final Duration DEFAULT_WAIT_TIME = findWaitingDuration();

    public static <T> void awaitForSize(Collection<T> collection, int expectedSize) {
        ConditionEvaluationListener<Boolean> listener = new ConditionEvaluationListener<>() {
            @Override
            public void conditionEvaluated(EvaluatedCondition condition) {

            }

            @Override
            public void onTimeout(TimeoutEvent timeoutEvent) {
                log.error("Timeout while waiting for size={} for collection={}", expectedSize, collection);
            }
        };

        Awaitility.await()
                .atMost(DEFAULT_WAIT_TIME)
                .conditionEvaluationListener(listener)
                .until(() -> collection.size() >= expectedSize);
    }

    private static Duration findWaitingDuration() {
        WatchImplementation implementation = WatchImplementation.determineImplementation();
        return implementation.getSensitivity().multipliedBy(10);
    }
}
