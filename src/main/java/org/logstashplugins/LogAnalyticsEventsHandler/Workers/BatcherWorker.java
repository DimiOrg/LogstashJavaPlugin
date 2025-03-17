package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.logstashplugins.LogstashLAHandlerEvent;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatcherWorker extends AbstractWorker<LogstashLAHandlerEvent> {
    private static final Logger logger = LoggerFactory.getLogger(BatcherWorker.class);
    
    private BlockingQueue<LAEventsHandlerEvent> eventsQueue;
    private BlockingQueue<List<Object>> batchesQueue;
    private LAEventsHandlerConfiguration.BatcherWorkerConfig configuration;
    
    public BatcherWorker(BlockingQueue<LAEventsHandlerEvent> eventsQueue, 
                        BlockingQueue<List<Object>> batchesQueue, 
                        LAEventsHandlerConfiguration.BatcherWorkerConfig configuration) {
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
                System.currentTimeMillis() - startTimeMillis < configuration.getMaxWaitingTimeSecondsForBatch() * 1000) {
            eventsQueue.drainTo(batch);
        }

        if (!batch.isEmpty()) {
            logger.debug("Adding batch to queue. Batch size: " + batch.size());
            batchesQueue.add(batch);
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down BatcherWorker. Thread id: " + Thread.currentThread().getId());

        running = false;

        // Create one last batch with remaining events in the queue
        List<Object> batch = new ArrayList<Object>();
        eventsQueue.drainTo(batch);
        if (!batch.isEmpty()) {
            logger.info("Adding last batch to queue. Batch size: " + batch.size());
            batchesQueue.add(batch);
        }
        logger.info("BatcherWorker shutdown complete. Thread id: " + Thread.currentThread().getId());
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}