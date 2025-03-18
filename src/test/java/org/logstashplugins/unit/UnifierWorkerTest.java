package org.logstashplugins.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.UnifierWorker;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UnifierWorkerTest {

    private BlockingQueue<List<Object>> batchesQueue;
    private BlockingQueue<List<Object>> unifiedBatchesQueue;
    private LAEventsHandlerConfiguration.UnifierWorkerConfig configuration;

    private UnifierWorker unifierWorker;

    @BeforeEach
    public void setUp() {
        configuration = new LAEventsHandlerConfiguration.UnifierWorkerConfig();
        configuration.setMaxWaitingForUnifierTimeSeconds(10);
        batchesQueue = new LinkedBlockingQueue<>();
        unifiedBatchesQueue = new LinkedBlockingQueue<>();
        unifierWorker = new UnifierWorker(batchesQueue, unifiedBatchesQueue, configuration);
    }

    @Test
    public void testProcess() throws InterruptedException {
        List<Object> batch1 = new ArrayList<>();
        batch1.add("event1");
        List<Object> batch2 = new ArrayList<>();
        batch2.add("event2");

        batchesQueue.add(batch1);
        batchesQueue.add(batch2);

        unifierWorker.process();

        assertEquals(0, batchesQueue.size());
        assertEquals(1, unifiedBatchesQueue.size());
        List<Object> unifiedBatch = unifiedBatchesQueue.poll();
        assertNotNull(unifiedBatch);
        assertEquals(2, unifiedBatch.size());
        assertTrue(unifiedBatch.contains("event1"));
        assertTrue(unifiedBatch.contains("event2"));
    }

    @Test
    public void testShutdown() {
        List<Object> batch1 = new ArrayList<>();
        batch1.add("event1");
        batchesQueue.add(batch1);

        assertTrue(unifierWorker.isRunning());

        unifierWorker.shutdown();

        assertEquals(0, batchesQueue.size());
        assertEquals(1, unifiedBatchesQueue.size());
        List<Object> unifiedBatch = unifiedBatchesQueue.poll();
        assertNotNull(unifiedBatch);
        assertEquals(1, unifiedBatch.size());
        assertTrue(unifiedBatch.contains("event1"));
        assertFalse(unifierWorker.isRunning());
    }
}