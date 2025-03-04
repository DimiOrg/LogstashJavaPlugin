package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

public interface Worker extends Runnable {
    void process() throws InterruptedException;
}
