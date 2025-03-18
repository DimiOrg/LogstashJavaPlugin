package org.logstashplugins.LogAnalyticsEventsHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

import org.logstashplugins.LogAnalyticsEventsHandler.Workers.BatcherWorker;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.SenderWorker;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.Worker;

public class LAEventsHandler {
    private static final Logger logger = LoggerFactory.getLogger(LAEventsHandler.class);

    private LAEventsHandlerConfiguration configuration;
    private BlockingQueue<LAEventsHandlerEvent> eventsQueue;
    private BlockingQueue<List<Object>> batchesQueue;
    private ScheduledExecutorService batchersExecutorService;
    private ScheduledExecutorService sendersExecutorService;
    private Set<BatcherWorker> batcherWorkers;
    private Set<SenderWorker> senderWorkers;

    public LAEventsHandler(LAEventsHandlerConfiguration configuration) {
        eventsQueue = new LinkedBlockingQueue<>();
        batchesQueue = new LinkedBlockingQueue<>();
        this.configuration = configuration;
        int batcherWorkerCount = getBatcherWorkerCount();
        int senderWorkerCount = getSenderWorkerCount();
        batchersExecutorService = Executors.newScheduledThreadPool(batcherWorkerCount); 
        sendersExecutorService = Executors.newScheduledThreadPool(batcherWorkerCount); 
        batcherWorkers = new HashSet<>();
        senderWorkers = new HashSet<>();

        logger.info("Starting Log Analytics Events Handler with {} batcher workers and {} sender workers", batcherWorkerCount, senderWorkerCount);
        
        // Start batcher workers and sender workers. The ScheduledExecutorService will make sure that terminated workers are restarted.
        for (int i = 0; i < batcherWorkerCount; i++) {
            BatcherWorker batcherWorker = new BatcherWorker(eventsQueue, batchesQueue, configuration.getBatcherWorkerConfig());
            batchersExecutorService.scheduleAtFixedRate(batcherWorker, 0, 1, TimeUnit.MINUTES);
            batcherWorkers.add(batcherWorker);
        }
        for (int i = 0; i < senderWorkerCount; i++) {
            SenderWorker senderWorker = new SenderWorker(batchesQueue, configuration.getSenderWorkerConfig());
            sendersExecutorService.scheduleAtFixedRate(senderWorker, i, 1, TimeUnit.MINUTES);
            senderWorkers.add(senderWorker);
        }

        logger.info("Log Analytics Events Handler has been started");
    }

    public void handle(LAEventsHandlerEvent event) {
        logger.debug("Events queue size: {}", eventsQueue.size());
        eventsQueue.add(event);
    }

    public void shutdown() {
        logger.info("Shutting down Log Analytics Events Handler");
        int shutdownTimeSeconds = Optional.ofNullable(configuration.getLaEventsHandlerConfig().getMaxGracefulShutdownTimeSeconds())
                           .filter(time -> time != 0)
                           .orElse(60);
        double batchersToSendersRatio = (double) batcherWorkers.size() / senderWorkers.size();
        int batchersMaxTimeToShutdown = (int) (shutdownTimeSeconds * batchersToSendersRatio);

        // call shutdown on workers to stop them gracefully. First the batchers, then the senders
        shutdownWorkersConcurrently(batcherWorkers, batchersMaxTimeToShutdown);
        logger.info("Batchers have been shut down");
        shutdownWorkersConcurrently(senderWorkers, shutdownTimeSeconds - batchersMaxTimeToShutdown);
        logger.info("Senders have been shut down");

        // shutdown the executor services
        batchersExecutorService.shutdown();
        sendersExecutorService.shutdown();

        try {
            if (!batchersExecutorService.awaitTermination(shutdownTimeSeconds , TimeUnit.SECONDS)) {
                logger.warn("Batchers executor service did not terminate in the specified time. Forcing shutdown.");
                batchersExecutorService.shutdownNow();
            }
            if (!sendersExecutorService.awaitTermination(shutdownTimeSeconds , TimeUnit.SECONDS)) {
                logger.warn("Senders executor service did not terminate in the specified time. Forcing shutdown.");
                sendersExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor services to terminate. Forcing shutdown.");
            batchersExecutorService.shutdownNow();
            sendersExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Log Analytics Events Handler has been shut down");
        for (BatcherWorker batcherWorker : batcherWorkers) {
            if (batcherWorker.isRunning()) {
                logger.warn("Batcher worker is still running");
            }
        }
        for (SenderWorker senderWorker : senderWorkers) {
            if (senderWorker.isRunning()) {
                logger.warn("Sender worker is still running");
            }
        }

        logger.debug("Events queue size: {}", eventsQueue.size());
        logger.debug("Batches queue size: {}", batchesQueue.size());
    }

    private void shutdownWorkersConcurrently(Set<? extends Worker> workers, int maxGracefulShutdownTimeSeconds) {
        ExecutorService shutdownExecutor = Executors.newFixedThreadPool(workers.size());
        for (Worker worker : workers) {
            shutdownExecutor.submit(worker::shutdown);
        }
        shutdownExecutor.shutdown();
        try {
            shutdownExecutor.awaitTermination(maxGracefulShutdownTimeSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            shutdownExecutor.shutdownNow();
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
