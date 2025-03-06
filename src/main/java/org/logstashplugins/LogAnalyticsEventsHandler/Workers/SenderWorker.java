package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

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

    public SenderWorker(BlockingQueue<List<Object>> batchesQueue, 
                        int initialDelaySeconds,
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


        //sleep for initialDelaySeconds before starting to process batches. Done in order to make senders start at different times.
        try {
            Thread.sleep(initialDelaySeconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process() throws InterruptedException {
        while(!batchesQueue.isEmpty()) {
            List<Object> batch = batchesQueue.take();
            try {
                client.upload(configuration.getDcrId(), configuration.getStreamName(), batch);
            } catch (Exception e) {
                // supress exceptions for now. Didn't implement any error handling yet. Don't want the worker to stop.
                e.printStackTrace();
            }
            
        }
    }
}
