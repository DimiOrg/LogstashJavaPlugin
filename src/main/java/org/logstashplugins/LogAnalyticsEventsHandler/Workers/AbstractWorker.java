package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

public abstract class AbstractWorker<T> implements Worker {
    @Override
    public void run() {
        while (true) {
            try {
                process();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
