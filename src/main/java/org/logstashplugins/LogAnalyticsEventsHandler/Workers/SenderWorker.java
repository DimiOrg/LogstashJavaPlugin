package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.TokenCredentialFactory;

import com.azure.core.credential.TokenCredential;
import com.azure.monitor.ingestion.LogsIngestionClient;
import com.azure.monitor.ingestion.LogsIngestionClientBuilder;

public class SenderWorker extends AbstractWorker<List<Object>> {
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
        running = false;

        // sleep for 5 seconds to allow the last batch to be sent
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get remaining batches and send them
        List<List<Object>> batches = new ArrayList<List<Object>>();
        batchesQueue.drainTo(batches);
        for(List<Object> batch : batches) {
            if(batch != null) {
                client.upload(configuration.getDcrId(), configuration.getStreamName(), batch);
            }            
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
