package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

public abstract class AbstractWorker<T> implements Worker {
    protected volatile boolean running;

    @Override
    public void run() {
        while (isRunning()) {
            try {
                process();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                shutdown();
                break;
            }
        }
    }
}
