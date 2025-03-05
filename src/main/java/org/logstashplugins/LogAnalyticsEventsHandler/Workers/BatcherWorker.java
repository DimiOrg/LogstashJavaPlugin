package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.logstashplugins.LogstashLAHandlerEvent;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;

public class BatcherWorker extends AbstractWorker<LogstashLAHandlerEvent> {
    private BlockingQueue<LAEventsHandlerEvent> eventsQueue;
    private BlockingQueue<List<Object>> batchesQueue;
    private LAEventsHandlerConfiguration configuration;
    
    public BatcherWorker(BlockingQueue<LAEventsHandlerEvent> eventsQueue, 
                        BlockingQueue<List<Object>> batchesQueue, 
                        LAEventsHandlerConfiguration configuration) {
        this.eventsQueue = eventsQueue; 
        this.batchesQueue = batchesQueue;
        this.configuration = configuration;
    }

    @Override
    public void process() throws InterruptedException {
        long startTimeMillis = System.currentTimeMillis();
        List<Object> batch = new ArrayList<Object>();
        
        while (System.currentTimeMillis() - startTimeMillis < configuration.getMaxWaitingTimeSeconds() * 1000) {
            LAEventsHandlerEvent event = eventsQueue.poll(5, TimeUnit.SECONDS);
            if (event != null) {
                batch.add(event.getLog());
            }
        }

        if (!batch.isEmpty()) {
            batchesQueue.add(batch);
        }
    }
}