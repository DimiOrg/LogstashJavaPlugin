package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.logstashplugins.LogAnalyticsEventsHandler.EventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.EventsHandlerEvent;

public class BatcherWorker extends AbstractWorker<EventsHandlerEvent> {
    private BlockingQueue<EventsHandlerEvent> eventsQueue;
    private BlockingQueue<List<Object>> batchesQueue;
    private EventsHandlerConfiguration configuration;
    
    public BatcherWorker(BlockingQueue<EventsHandlerEvent> eventsQueue, 
                        BlockingQueue<List<Object>> batchesQueue, 
                        EventsHandlerConfiguration configuration) {
        this.eventsQueue = eventsQueue; 
        this.batchesQueue = batchesQueue;
        this.configuration = configuration;
    }

    @Override
    public void process() throws InterruptedException {
        int currentTimeMillis = (int) System.currentTimeMillis();
        int currentEventsCount = 0;
        List<Object> batch = new ArrayList<Object>();
        
        while (currentEventsCount < configuration.getMaxEventsPerBatch() && (int) System.currentTimeMillis() - currentTimeMillis < configuration.getMaxWaitingTimeSeconds() * 1000) {
            EventsHandlerEvent event = eventsQueue.poll(60, TimeUnit.SECONDS);
            if (event != null) {
                batch.add(event.getLog());
                currentEventsCount++;
            }
            currentTimeMillis = (int) System.currentTimeMillis();
        }

        if (!batch.isEmpty()) {
            batchesQueue.add(batch);
        }
    }
}