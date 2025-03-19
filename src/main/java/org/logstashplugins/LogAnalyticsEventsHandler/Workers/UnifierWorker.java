package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnifierWorker extends AbstractWorker<List<Object>> {
    private static final Logger logger = LoggerFactory.getLogger(UnifierWorker.class);
    
    private BlockingQueue<List<Object>> batchesQueue;
    private BlockingQueue<List<Object>> unifiedBatchesQueue;
    private LAEventsHandlerConfiguration.UnifierWorkerConfig configuration;
    
    public UnifierWorker(BlockingQueue<List<Object>> batchesQueue, 
                        BlockingQueue<List<Object>> unifiedBatchesQueue, 
                        LAEventsHandlerConfiguration.UnifierWorkerConfig configuration) {
        this.batchesQueue = batchesQueue;
        this.unifiedBatchesQueue = unifiedBatchesQueue;
        this.configuration = configuration;
        this.running = true;
    }

    @Override
    public void process() throws InterruptedException {
        long startTimeMillis = System.currentTimeMillis();
        List<List<Object>> originalBatches = new ArrayList<List<Object>>();

        while(running && !Thread.currentThread().isInterrupted() && 
            System.currentTimeMillis() - startTimeMillis < configuration.getMaxWaitingForUnifierTimeSeconds() * 1000) {
            batchesQueue.drainTo(originalBatches);
            Thread.sleep(10);
        }

        unifyAndSendBatch(originalBatches);
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down UnifierWorker. Thread id: " + Thread.currentThread().getId());

        running = false;

        // Create one last unified batch with remaining batches in the queue
        List<List<Object>> originalBatches = new ArrayList<List<Object>>();
        batchesQueue.drainTo(originalBatches);
        unifyAndSendBatch(originalBatches);
        logger.info("UnifierWorker shutdown complete. Thread id: " + Thread.currentThread().getId());
    }

    @Override
    public boolean isRunning() {
        return running;
    }
    
    private void unifyAndSendBatch(List<List<Object>> originalBatches) {
        if(originalBatches.isEmpty()) {
            return;
        }
        List<Object> unifiedBatch = new ArrayList<Object>();
        logger.debug("Unifying " + originalBatches.size() + " batches");
        for(List<Object> batch : originalBatches) {
            unifiedBatch.addAll(batch);
        }
        unifiedBatchesQueue.add(unifiedBatch);
    }
}
