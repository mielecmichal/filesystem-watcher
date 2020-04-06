package io.github.filesystemwatcher.utilities;

import java.util.concurrent.CountDownLatch;

public class  WatchCoordinator {
    private final CountDownLatch watcherLatch = new CountDownLatch(1);
    private final CountDownLatch setupLatch = new CountDownLatch(1);

    public void watcherCompleted(){
        watcherLatch.countDown();
    }

    public void setupCompleted(){
        setupLatch.countDown();
        awaitWatcher();
    }

    void awaitWatcher() {
        try {
            watcherLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void awaitSetup() {
        try {
            setupLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}