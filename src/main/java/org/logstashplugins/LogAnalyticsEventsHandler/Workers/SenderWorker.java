package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.logstashplugins.LogAnalyticsEventsHandler.EventsHandlerConfiguration;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.ingestion.LogsIngestionClient;
import com.azure.monitor.ingestion.LogsIngestionClientBuilder;

public class SenderWorker extends AbstractWorker<List<Object>> {
    private BlockingQueue<List<Object>> batchesQueue;
    private LogsIngestionClient client;
    private EventsHandlerConfiguration configuration;

    public SenderWorker(BlockingQueue<List<Object>> batchesQueue, 
                        int initialDelaySeconds,
                        EventsHandlerConfiguration configuration) {
        this.batchesQueue = batchesQueue;
        this.client = new LogsIngestionClientBuilder()
                .credential(new DefaultAzureCredentialBuilder().build())
                .endpoint(configuration.getDataCollectionEndpoint())
                .buildClient();
        this.configuration = configuration;

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
            client.upload(configuration.getDcrId(), configuration.getTableName(), batch);
        }
    }
}
