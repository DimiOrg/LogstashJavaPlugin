package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWorker<T> implements Worker {
    private static final Logger logger = LoggerFactory.getLogger(AbstractWorker.class);
    protected volatile boolean running;

    @Override
    public void run() {
        while (isRunning()) {
            try {
                process();
            } catch (InterruptedException e) {
                logger.error("Worker interrupted", e);
                Thread.currentThread().interrupt();
                shutdown();
                break;
            }
        }
    }
}
