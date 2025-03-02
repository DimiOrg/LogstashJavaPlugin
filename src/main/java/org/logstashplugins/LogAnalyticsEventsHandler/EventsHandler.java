package org.logstashplugins.LogAnalyticsEventsHandler;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.logstashplugins.LogAnalyticsEventsHandler.Workers.BatcherWorker;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.SenderWorker;

public class EventsHandler {
    private EventsHandlerConfiguration configuration;
    private BlockingQueue<EventsHandlerEvent> eventsQueue;
    private BlockingQueue<List<Object>> batchesQueue;

    public EventsHandler(EventsHandlerConfiguration configuration) {
        eventsQueue = new LinkedBlockingQueue<EventsHandlerEvent>();
        batchesQueue = new LinkedBlockingQueue<List<Object>>();
        this.configuration = configuration;

        for (int i = 0; i < 3; i++) {
            new Thread(new BatcherWorker(eventsQueue, batchesQueue, configuration)).start();
            new Thread(new SenderWorker(batchesQueue, 1000 * i, configuration)).start();
        }
    }

    public void handle(EventsHandlerEvent event) {
        eventsQueue.add(event);
    }
}
