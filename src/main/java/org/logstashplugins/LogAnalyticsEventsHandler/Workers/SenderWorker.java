package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.TokenCredentialFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.HttpResponseException;
import com.azure.monitor.ingestion.LogsIngestionClient;
import com.azure.monitor.ingestion.LogsIngestionClientBuilder;
import com.azure.monitor.ingestion.models.LogsUploadException;

public class SenderWorker extends AbstractWorker<List<Object>> {
    private static final Logger logger = LoggerFactory.getLogger(SenderWorker.class);
    
    private BlockingQueue<List<Object>> batchesQueue;
    private LogsIngestionClient client;
    private LAEventsHandlerConfiguration.SenderWorkerConfig configuration;
    
    public SenderWorker(BlockingQueue<List<Object>> batchesQueue, 
                        LAEventsHandlerConfiguration.SenderWorkerConfig configuration) {
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

    // contructor for testing
    public SenderWorker(BlockingQueue<List<Object>> batchesQueue, 
                        LAEventsHandlerConfiguration.SenderWorkerConfig configuration,
                        LogsIngestionClient client) {
        this.configuration = configuration;
        this.batchesQueue = batchesQueue;
        this.client = client;
        this.running = true;
    }

    @Override
    public void process() throws InterruptedException {
        while(running && !Thread.currentThread().isInterrupted() && !batchesQueue.isEmpty()) {
            List<List<Object>> batches = new ArrayList<List<Object>>();
            batchesQueue.drainTo(batches);
            logger.debug("Sending " + batches.size() + " batches to Log Analytics");
            for(List<Object> batch : batches) {         
                if (!uploadWithExpBackoffRetries(batch, configuration.getMaxRetriesNum(), configuration.getInitialWaitTimeSeconds())) {
                    // If upload fails, write a log with the batch size and drop the batch. later on we will implement a DLQ
                    logger.error("Failed to upload batch. Dropping batch. Batch size: " + batch.size());
                }            
            }
            // Sleep for a short time to avoid busy waiting
            Thread.sleep(10);
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
                try{
                    client.upload(configuration.getDcrId(), configuration.getStreamName(), batch);
                } catch (Exception e) {
                    // On shutdown, we don't want to retry. Just log the error and continue
                    logger.error("Failed to upload batch on shutdown", e);
                }            
            }
        }
        logger.info("SenderWorker shutdown complete. Thread id: " + Thread.currentThread().getId());
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private boolean uploadWithExpBackoffRetries(List<Object> batch, int maxRetries, long initialWaitTime) {
        boolean success = false;
        int retries = 0;
        long waitTime = initialWaitTime;
    
        while (!success && retries < maxRetries) {
            try {
                logger.debug("Uploading batch. Batch size: " + batch.size());
                client.upload(configuration.getDcrId(), configuration.getStreamName(), batch);
                success = true; // If upload is successful, exit the loop
            } catch (Exception e) {
                if (!isRetryableException(e)) {
                    // Write to log and exit the loop
                    logger.error("Failed to upload batch. Exception is non retryable", e);
                    return false;
                }
                retries++;
                if (retries < maxRetries) {
                    logger.debug("Failed to upload batch. Retrying. retry number: " + retries);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false; // If interrupted, exit the method
                    }
                    waitTime *= 2; // Exponential backoff
                } else {
                    // Handle the failure after max retries
                    logger.error("Failed to upload batch after " + maxRetries + " attempts");
                }
            }
        }
        return success;
    }

    private boolean isRetryableException(Exception e) {
        // Must be a subtype of LogsUploadException to be retryable
        return e instanceof LogsUploadException && containsRetryableHttpStatusCodes((LogsUploadException) e);
    }

    private boolean containsRetryableHttpStatusCodes(LogsUploadException e) {
        // Iterate over the logsUploadErrors to check if they are all retryable
        for (HttpResponseException httpException : e.getLogsUploadErrors()) {
            if (!isRetryableHttpStatusCode(httpException)) {
                return false;
            }
        }
        return true;
    }

    private boolean isRetryableHttpStatusCode(HttpResponseException e) {
        int statusCode = e.getResponse().getStatusCode();
        return statusCode == 429 || statusCode == 500 || statusCode == 503;
    }
}
