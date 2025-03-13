package org.logstashplugins.LogAnalyticsEventsHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

import org.logstashplugins.LogAnalyticsEventsHandler.Workers.BatcherWorker;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.SenderWorker;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.Worker;

public class LAEventsHandler {
    private LAEventsHandlerConfiguration configuration;
    private BlockingQueue<LAEventsHandlerEvent> eventsQueue;
    private BlockingQueue<List<Object>> batchesQueue;
    private ScheduledExecutorService batchersExecutorService;
    private ScheduledExecutorService sendersExecutorService;
    private Set<Worker> workers;

    public LAEventsHandler(LAEventsHandlerConfiguration configuration) {
        eventsQueue = new LinkedBlockingQueue<>();
        batchesQueue = new LinkedBlockingQueue<>();
        this.configuration = configuration;
        int batcherWorkerCount = getBatcherWorkerCount();
        int senderWorkerCount = getSenderWorkerCount();
        batchersExecutorService = Executors.newScheduledThreadPool(batcherWorkerCount); 
        sendersExecutorService = Executors.newScheduledThreadPool(batcherWorkerCount); 
        workers = new HashSet<>();

        // Start batcher workers and sender workers. The ScheduledExecutorService will make sure that terminated workers are restarted.
        for (int i = 0; i < batcherWorkerCount; i++) {
            BatcherWorker batcherWorker = new BatcherWorker(eventsQueue, batchesQueue, configuration);
            batchersExecutorService.scheduleAtFixedRate(batcherWorker, 0, 1, TimeUnit.MINUTES);
            workers.add(batcherWorker);
        }
        for (int i = 0; i < senderWorkerCount; i++) {
            SenderWorker senderWorker = new SenderWorker(batchesQueue, configuration);
            sendersExecutorService.scheduleAtFixedRate(senderWorker, i, 1, TimeUnit.MINUTES);
            workers.add(senderWorker);
        }
    }

    public void handle(LAEventsHandlerEvent event) {
        eventsQueue.add(event);
    }

    public void shutdown() {
        workers.forEach(Worker::shutdown);
        batchersExecutorService.shutdown();
        sendersExecutorService.shutdown();

        int shutdownTimeSeconds = Optional.ofNullable(configuration.getMaxGracefulShutdownTimeSeconds())
                           .filter(time -> time != 0)
                           .orElse(60);
        try {
            if (!batchersExecutorService.awaitTermination(shutdownTimeSeconds , TimeUnit.SECONDS)) {
                batchersExecutorService.shutdownNow();
            }
            if (!sendersExecutorService.awaitTermination(shutdownTimeSeconds , TimeUnit.SECONDS)) {
                sendersExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchersExecutorService.shutdownNow();
            sendersExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private int getBatcherWorkerCount() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        return (availableProcessors - 1) / 2;
    }

    private int getSenderWorkerCount() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        return (availableProcessors - 1) / 2;
    }
}
