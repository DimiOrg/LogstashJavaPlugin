package org.logstashplugins.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.BatcherWorker;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BatcherWorkerTest {

    private BlockingQueue<LAEventsHandlerEvent> inputQueue;
    private BlockingQueue<List<Object>> sendersQueue;
    private BlockingQueue<List<Object>> unifiersQueue;
    private LAEventsHandlerConfiguration.BatcherWorkerConfig configuration;

    private BatcherWorker batcherWorker;

    @BeforeEach
    public void setUp() {
        configuration = new LAEventsHandlerConfiguration.BatcherWorkerConfig();
        configuration.setMaxWaitingTimeSecondsForBatch(10);
        inputQueue = new LinkedBlockingQueue<>();
        sendersQueue = new LinkedBlockingQueue<>();
        unifiersQueue = new LinkedBlockingQueue<>();
        batcherWorker = new BatcherWorker(inputQueue, sendersQueue, unifiersQueue, configuration);
    }

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
    public void testShutdown() {
        LAEventsHandlerEvent event1 = mock(LAEventsHandlerEvent.class);
        inputQueue.add(event1);

        assertTrue(batcherWorker.isRunning());

        batcherWorker.shutdown();

        assertEquals(0, inputQueue.size());
        assertEquals(1, sendersQueue.size());
        List<Object> batch = sendersQueue.poll();
        assertNotNull(batch);
        assertEquals(1, batch.size());
        assertTrue(batch.contains(event1));
        assertFalse(batcherWorker.isRunning());
    }
}
