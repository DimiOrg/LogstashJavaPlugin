package org.logstashplugins.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.BatcherWorker;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BatcherWorkerTest {
    private static final Logger logger = LoggerFactory.getLogger(BatcherWorkerTest.class);

    private BlockingQueue<LAEventsHandlerEvent> inputQueue;
    private BlockingQueue<List<Object>> sendersQueue;
    private BlockingQueue<List<Object>> unifiersQueue;
    private LAEventsHandlerConfiguration.BatcherWorkerConfig configuration;

    private BatcherWorker batcherWorker;

    @BeforeEach
    public void setUp() {
        configuration = new LAEventsHandlerConfiguration.BatcherWorkerConfig();
        configuration.setMaxWaitingTimeSecondsForBatch(10);
        configuration.setSleepTimeMillis(100);  // Set shorter sleep time for testing
        inputQueue = new LinkedBlockingQueue<>();
        sendersQueue = new LinkedBlockingQueue<>();
        unifiersQueue = new LinkedBlockingQueue<>();
        batcherWorker = new BatcherWorker(inputQueue, sendersQueue, unifiersQueue, configuration);
    }

    
    @Disabled("Temporarily disabled due to potential infinite loop")
    @Test
    public void testProcessLowTraffic() throws InterruptedException {
        LAEventsHandlerEvent event1 = mock(LAEventsHandlerEvent.class);
        LAEventsHandlerEvent event2 = mock(LAEventsHandlerEvent.class);

        // Process LOW_TRAFFIC_CALCULATION_FREQUENCY times so that in the next process call, isLowTraffic will be calculated
        for(int i=0; i < BatcherWorker.LOW_TRAFFIC_CALCULATION_FREQUENCY; i++) {
            batcherWorker.process();
        }

        inputQueue.add(event1);
        inputQueue.add(event2);

        batcherWorker.process();        

        assertEquals(0, inputQueue.size());
        assertEquals(1, unifiersQueue.size());
        List<Object> batch = unifiersQueue.poll();
        assertNotNull(batch);
        assertEquals(2, batch.size());
        assertTrue(batch.contains(event1));
        assertTrue(batch.contains(event2));
    }

    @Test
    public void testProcessHighTraffic() throws InterruptedException {
        LAEventsHandlerEvent event1 = mock(LAEventsHandlerEvent.class);
        LAEventsHandlerEvent event2 = mock(LAEventsHandlerEvent.class);

        inputQueue.add(event1);
        inputQueue.add(event2);

        batcherWorker.process();

        assertEquals(0, inputQueue.size());
        assertEquals(1, sendersQueue.size());
        List<Object> batch = sendersQueue.poll();
        assertNotNull(batch);
        assertEquals(2, batch.size());
        assertTrue(batch.contains(event1));
        assertTrue(batch.contains(event2));
    }

    @Test
    public void testShutdown() throws InterruptedException {
        logger.error("TEST START: Starting shutdown test with multiple workers...");
        
        // Create multiple workers sharing the same queues
        BatcherWorker worker1 = new BatcherWorker(inputQueue, sendersQueue, unifiersQueue, configuration);
        BatcherWorker worker2 = new BatcherWorker(inputQueue, sendersQueue, unifiersQueue, configuration);
        
        logger.error("Created workers");

        // Add test events that should be processed
        for (int i = 0; i < 5; i++) {
            inputQueue.add(mock(LAEventsHandlerEvent.class));
        }
        logger.error("Added 5 initial events");

        // Process events with both workers
        worker1.process();
        worker2.process();

        // Verify initial processing works
        assertTrue(inputQueue.isEmpty(), "Initial events should be processed");
        assertTrue(sendersQueue.size() > 0 || unifiersQueue.size() > 0, "Events should be processed");
        int processedEvents = sendersQueue.size() + unifiersQueue.size();
        logger.error("Initial events were processed: " + processedEvents + " batches created");

        // Clear output queues for next phase
        sendersQueue.clear();
        unifiersQueue.clear();

        // Shutdown worker1
        logger.error("Shutting down worker1...");
        worker1.shutdown();

        // Add post-shutdown events
        for (int i = 0; i < 5; i++) {
            inputQueue.add(mock(LAEventsHandlerEvent.class));
        }
        logger.error("Added 5 post-shutdown events");

        // Try to process with both workers
        worker1.process();  // Should not process (shutdown)
        worker2.process();  // Should not process (shared shutdown state)

        // Verify post-shutdown state
        assertEquals(5, inputQueue.size(), "Post-shutdown events should remain in input queue");
        assertEquals(0, sendersQueue.size() + unifiersQueue.size(), "No events should be processed after shutdown");
        assertFalse(worker1.isRunning(), "Worker1 should be stopped");
        assertFalse(worker2.isRunning(), "Worker2 should be stopped due to shared state");

        // Log final state for debugging
        logger.error("After shutdown state:");
        logger.error("- Events in input queue: " + inputQueue.size());
        logger.error("- Events processed after shutdown: " + (sendersQueue.size() + unifiersQueue.size()));
        logger.error("- Worker1 running: " + worker1.isRunning());
        logger.error("- Worker2 running: " + worker2.isRunning());

        // Clean up
        worker2.shutdown();
    }
}
