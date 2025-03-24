package org.logstashplugins.LogAnalyticsEventsHandler.Workers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPOutputStream;

import org.logstashplugins.LogstashLAHandlerEvent;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.util.serializer.JsonSerializer;
import com.azure.core.util.serializer.JsonSerializerProviders;
import com.azure.json.JsonProviders;
import com.azure.json.JsonWriter;

public class BatcherWorker extends AbstractWorker<LogstashLAHandlerEvent> {
    private static final Logger logger = LoggerFactory.getLogger(BatcherWorker.class);
    private static final JsonSerializer DEFAULT_SERIALIZER = JsonSerializerProviders.createInstance(true);
    private static final int LOW_TRAFFIC_MAX_PAYLOAD_SIZE = 100 * 1024; // 100 KB
    public static final int LOW_TRAFFIC_CALCULATION_FREQUENCY = 10; // every 10 calls to process

    private BlockingQueue<LAEventsHandlerEvent> inputQueue;
    private BlockingQueue<List<Object>> unifiersQueue;
    private BlockingQueue<List<Object>> sendersQueue;
    private LAEventsHandlerConfiguration.BatcherWorkerConfig configuration;
    private int counter = 0;

    private boolean isLowTraffic;
    
    public BatcherWorker(BlockingQueue<LAEventsHandlerEvent> inputQueue, 
                        BlockingQueue<List<Object>> sendersQueue, 
                        BlockingQueue<List<Object>> unifiersQueue,
                        LAEventsHandlerConfiguration.BatcherWorkerConfig configuration) {
        this.inputQueue = inputQueue; 
        this.unifiersQueue = unifiersQueue;
        this.sendersQueue = sendersQueue;
        this.configuration = configuration;
        this.running = true;
        this.isLowTraffic = false;
    }

    @Override
    public void process() throws InterruptedException {      
        long startTimeMillis = System.currentTimeMillis();
        List<Object> batch = new ArrayList<Object>();
        
        while (running && !Thread.currentThread().isInterrupted() && 
                System.currentTimeMillis() - startTimeMillis < configuration.getMaxWaitingTimeSecondsForBatch() * 1000) {
            inputQueue.drainTo(batch);
            // Sleep for a short time to avoid busy waiting
            Thread.sleep(10);
        }

        if (!batch.isEmpty()) {
            // calculate traffic every LOW_TRAFFIC_CALCULATION_FREQUENCY calls to process
            if (counter >= LOW_TRAFFIC_CALCULATION_FREQUENCY) {
                calculateTraffic(batch);
                counter = 0;
            }

            if(isLowTraffic) {
                logger.debug("Adding batch to unifier queue. Batch size: " + batch.size());
                unifiersQueue.add(batch);
            } else {
                logger.debug("Adding batch to senders queue. Batch size: " + batch.size());
                sendersQueue.add(batch);
            }
        }
        counter++;
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down BatcherWorker. Thread id: " + Thread.currentThread().getId());

        running = false;

        // Create one last batch with remaining events in the queue
        List<Object> batch = new ArrayList<Object>();
        inputQueue.drainTo(batch);
        if (!batch.isEmpty()) {
            logger.info("Adding last batch to queue. Batch size: " + batch.size());
            sendersQueue.add(batch);
        }
        logger.info("BatcherWorker shutdown complete. Thread id: " + Thread.currentThread().getId());
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /*
     * Calculate traffic based on batch size. eventually batches are going to be sent by com.azure.monitor.ingestion.LogsIngestionClient.upload. this function
     * eventually uses com.azure.monitor.ingestion.implementation.Batcher.createRequest to gzip the logs inside the batch and send them to the endpoint.
     * so we can calculate the traffic based on doing the same gzip operation on the batch and measuring the size of the result. if the size is less than
     * LOW_TRAFFIC_MAX_PAYLOAD_SIZE, we can set isLowTraffic to true.
     */
    private void calculateTraffic(List<Object> batch) {
        // serialize logs
        List<String> serializedLogs = new ArrayList<>();        
        for (Object log : batch) {
            byte[] bytes = DEFAULT_SERIALIZER.serializeToBytes(log);
            serializedLogs.add(new String(bytes, StandardCharsets.UTF_8));
        }

        // gzip logs
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JsonWriter writer = JsonProviders.createWriter(byteArrayOutputStream)) {
            writer.writeStartArray();
            for (String log : serializedLogs) {
                writer.writeRawValue(log);
            }
            writer.writeEndArray();
            writer.flush();
            byte[] zippedRequestBody = gzipRequest(byteArrayOutputStream.toByteArray());

            // check if the size of the zipped logs is less than LOW_TRAFFIC_MAX_PAYLOAD_SIZE
            isLowTraffic = zippedRequestBody.length < LOW_TRAFFIC_MAX_PAYLOAD_SIZE;
            logger.debug("isLowTraffic: {}", isLowTraffic);
        } catch (Exception e) {
            logger.error("Failed to gzip logs. Assuming isLowTraffic=false", e);
            isLowTraffic = false;
        }
    }

    private static byte[] gzipRequest(byte[] bytes) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream zip = new GZIPOutputStream(byteArrayOutputStream)) {
            zip.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
