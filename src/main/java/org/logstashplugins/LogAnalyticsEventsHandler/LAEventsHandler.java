package org.logstashplugins.LogAnalyticsEventsHandler;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.logstashplugins.LogAnalyticsEventsHandler.Workers.BatcherWorker;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.SenderWorker;

public class LAEventsHandler {
    private LAEventsHandlerConfiguration configuration;
    private BlockingQueue<LAEventsHandlerEvent> eventsQueue;
    private BlockingQueue<List<Object>> batchesQueue;

    public LAEventsHandler(LAEventsHandlerConfiguration configuration) {
        eventsQueue = new LinkedBlockingQueue<LAEventsHandlerEvent>();
        batchesQueue = new LinkedBlockingQueue<List<Object>>();
        this.configuration = configuration;

        for (int i = 0; i < 3; i++) {
            new Thread(new BatcherWorker(eventsQueue, batchesQueue, configuration)).start();
            new Thread(new SenderWorker(batchesQueue, i, configuration)).start();
        }
    }

    public void handle(LAEventsHandlerEvent event) {
        eventsQueue.add(event);
    }
}
