package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.logstashplugins.LogstashLAHandlerEvent;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;

public class BatcherWorker extends AbstractWorker<LogstashLAHandlerEvent> {
    private BlockingQueue<LAEventsHandlerEvent> eventsQueue;
    private BlockingQueue<List<Object>> batchesQueue;
    private LAEventsHandlerConfiguration configuration;
    private boolean running;
    
    public BatcherWorker(BlockingQueue<LAEventsHandlerEvent> eventsQueue, 
                        BlockingQueue<List<Object>> batchesQueue, 
                        LAEventsHandlerConfiguration configuration) {
        this.eventsQueue = eventsQueue; 
        this.batchesQueue = batchesQueue;
        this.configuration = configuration;
        this.running = true;
    }

    @Override
    public void process() throws InterruptedException {
        long startTimeMillis = System.currentTimeMillis();
        List<Object> batch = new ArrayList<Object>();
        
        while (running && !Thread.currentThread().isInterrupted() && 
                System.currentTimeMillis() - startTimeMillis < configuration.getMaxWaitingTimeSeconds() * 1000) {
            eventsQueue.drainTo(batch);
        }

        if (!batch.isEmpty()) {
            batchesQueue.add(batch);
        }
    }

    @Override
    public void shutdown() {
        running = false;

        // Create one last batch with remaining events in the queue
        List<Object> batch = new ArrayList<Object>();
        eventsQueue.drainTo(batch);
        if (!batch.isEmpty()) {
            batchesQueue.add(batch);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}