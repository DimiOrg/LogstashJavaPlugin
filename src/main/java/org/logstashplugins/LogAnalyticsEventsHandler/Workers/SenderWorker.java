package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.TokenCredentialFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.credential.TokenCredential;
import com.azure.monitor.ingestion.LogsIngestionClient;
import com.azure.monitor.ingestion.LogsIngestionClientBuilder;

public class SenderWorker extends AbstractWorker<List<Object>> {
    private static final Logger logger = LoggerFactory.getLogger(SenderWorker.class);
    
    private BlockingQueue<List<Object>> batchesQueue;
    private LogsIngestionClient client;
    private LAEventsHandlerConfiguration configuration;
    private boolean running;
    
    public SenderWorker(BlockingQueue<List<Object>> batchesQueue, 
                        LAEventsHandlerConfiguration configuration) {
        this.configuration = configuration;
        this.batchesQueue = batchesQueue;

        TokenCredential tokenCredential = TokenCredentialFactory.createCredential(
            configuration.getAuthenticationType(),
            configuration.getClientId(),
            configuration.getClientSecret(),
            configuration.getTenantId()
        );
        this.client = new LogsIngestionClientBuilder()
                .credential(tokenCredential)
                .endpoint(configuration.getDataCollectionEndpoint())
                .buildClient();
        
        this.running = true;
    }

    @Override
    public void process() throws InterruptedException {
        while(running && !Thread.currentThread().isInterrupted() && !batchesQueue.isEmpty()) {
            List<List<Object>> batches = new ArrayList<List<Object>>();
            batchesQueue.drainTo(batches);
            for(List<Object> batch : batches) {
                if(batch != null) {
                    client.upload(configuration.getDcrId(), configuration.getStreamName(), batch);
                }            
            }         
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down SenderWorker. Thread id: " + Thread.currentThread().getId());

        running = false;

        // Get remaining batches and send them
        List<List<Object>> batches = new ArrayList<List<Object>>();
        batchesQueue.drainTo(batches);
        for(List<Object> batch : batches) {
            if(batch != null) {
                client.upload(configuration.getDcrId(), configuration.getStreamName(), batch);
            }            
        }

        logger.info("SenderWorker shutdown complete. Thread id: " + Thread.currentThread().getId());
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
