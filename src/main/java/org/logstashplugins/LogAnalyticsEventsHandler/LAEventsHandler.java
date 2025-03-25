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
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.UnifierWorker;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.Worker;

public class LAEventsHandler {
    private static final Logger logger = LoggerFactory.getLogger(LAEventsHandler.class);

    private LAEventsHandlerConfiguration configuration;

    private BlockingQueue<LAEventsHandlerEvent> inputQueue;
    private BlockingQueue<List<Object>> sendersQueue;
    private BlockingQueue<List<Object>> unifiersQueue;

    private ScheduledExecutorService batchersExecutorService;
    private ScheduledExecutorService sendersExecutorService;
    private ScheduledExecutorService unifiersExecutorService;
    private Set<BatcherWorker> batcherWorkers;
    private Set<SenderWorker> senderWorkers;
    private Set<UnifierWorker> unifierWorkers;

    public LAEventsHandler(LAEventsHandlerConfiguration configuration) {
        inputQueue = new LinkedBlockingQueue<>();
        unifiersQueue = new LinkedBlockingQueue<>();
        sendersQueue = new LinkedBlockingQueue<>();

        this.configuration = configuration;
        int batcherWorkerCount = getBatcherWorkerCount();
        int senderWorkerCount = getSenderWorkerCount();
        int unifierWorkerCount = getUnifierWorkerCount();
        batchersExecutorService = Executors.newScheduledThreadPool(batcherWorkerCount); 
        sendersExecutorService = Executors.newScheduledThreadPool(batcherWorkerCount); 
        unifiersExecutorService = Executors.newScheduledThreadPool(unifierWorkerCount);
        batcherWorkers = new HashSet<>();
        senderWorkers = new HashSet<>();
        unifierWorkers = new HashSet<>();
        logger.info("Starting Log Analytics Events Handler with {} batcher workers and {} sender workers and {} unifier workers", batcherWorkerCount, senderWorkerCount, unifierWorkerCount);
        
        // Start batcher workers and sender workers. The ScheduledExecutorService will make sure that terminated workers are restarted.
        for (int i = 0; i < batcherWorkerCount; i++) {
            BatcherWorker batcherWorker = new BatcherWorker(inputQueue, sendersQueue, unifiersQueue, configuration.getBatcherWorkerConfig());
            batchersExecutorService.scheduleAtFixedRate(batcherWorker, 0, 1, TimeUnit.MINUTES);
            batcherWorkers.add(batcherWorker);
        }
        for (int i = 0; i < senderWorkerCount; i++) {
            SenderWorker senderWorker = new SenderWorker(sendersQueue, configuration.getSenderWorkerConfig());
            sendersExecutorService.scheduleAtFixedRate(senderWorker, i, 1, TimeUnit.MINUTES);
            senderWorkers.add(senderWorker);
        }
        for (int i = 0; i < unifierWorkerCount; i++) {
            UnifierWorker unifierWorker = new UnifierWorker(unifiersQueue, sendersQueue, configuration.getUnifierWorkerConfig());
            unifiersExecutorService.scheduleAtFixedRate(unifierWorker, 0, 1, TimeUnit.MINUTES);
            unifierWorkers.add(unifierWorker);
        }

        logger.info("Log Analytics Events Handler has been started");
    }

    public void handle(LAEventsHandlerEvent event) {
        logger.debug("Events queue size: {}", inputQueue.size());
        inputQueue.add(event);
    }

    public void shutdown() {
        logger.info("Shutting down Log Analytics Events Handler");
        int shutdownTimeSeconds = Optional.ofNullable(configuration.getLaEventsHandlerConfig().getMaxGracefulShutdownTimeSeconds())
                           .filter(time -> time != 0)
                           .orElse(60);

        int totalWorkerCount = batcherWorkers.size() + senderWorkers.size() + unifierWorkers.size();
        int batchersMaxTimeToShutdown = shutdownTimeSeconds * (batcherWorkers.size() / totalWorkerCount);
        int unifiersMaxTimeToShutdown = shutdownTimeSeconds * (unifierWorkers.size() / totalWorkerCount);
        int sendersMaxTimeToShutdown = shutdownTimeSeconds * (senderWorkers.size() / totalWorkerCount);

        // call shutdown on workers to stop them gracefully. First the batchers, then the unifiers and finally the senders
        shutdownWorkersConcurrently(batcherWorkers, batchersMaxTimeToShutdown);
        logger.info("Batchers have been shut down");
        shutdownWorkersConcurrently(unifierWorkers, unifiersMaxTimeToShutdown);
        logger.info("Unifiers have been shut down");
        shutdownWorkersConcurrently(senderWorkers, sendersMaxTimeToShutdown);
        logger.info("Senders have been shut down");

        // shutdown the executor services
        batchersExecutorService.shutdown();
        unifiersExecutorService.shutdown();
        sendersExecutorService.shutdown();

        try {
            if (!batchersExecutorService.awaitTermination(shutdownTimeSeconds , TimeUnit.SECONDS)) {
                logger.warn("Batchers executor service did not terminate in the specified time. Forcing shutdown.");
                batchersExecutorService.shutdownNow();
            }
            if (!unifiersExecutorService.awaitTermination(shutdownTimeSeconds , TimeUnit.SECONDS)) {
                logger.warn("Unifiers executor service did not terminate in the specified time. Forcing shutdown.");
                unifiersExecutorService.shutdownNow();
            }
            if (!sendersExecutorService.awaitTermination(shutdownTimeSeconds , TimeUnit.SECONDS)) {
                logger.warn("Senders executor service did not terminate in the specified time. Forcing shutdown.");
                sendersExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor services to terminate. Forcing shutdown.");
            batchersExecutorService.shutdownNow();
            sendersExecutorService.shutdownNow();
            unifiersExecutorService.shutdownNow();
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
        for (UnifierWorker unifierWorker : unifierWorkers) {
            if (unifierWorker.isRunning()) {
                logger.warn("Unifier worker is still running");
            }
        }

        logger.debug("Input queue size: {}", inputQueue.size());
        logger.debug("Unifiers queue size: {}", unifiersQueue.size());
        logger.debug("Senders queue size: {}", sendersQueue.size());
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
        if (configuration.getLaEventsHandlerConfig().getBatcherWorkersCount() != null) {
            return configuration.getLaEventsHandlerConfig().getBatcherWorkersCount();
        }
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        return (availableProcessors - 1) / 3;
    }

    private int getSenderWorkerCount() {
        if (configuration.getLaEventsHandlerConfig().getSenderWorkersCount() != null) {
            return configuration.getLaEventsHandlerConfig().getSenderWorkersCount();
        }
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        return (availableProcessors - 1) / 3;
    }

    private int getUnifierWorkerCount() {
        if (configuration.getLaEventsHandlerConfig().getUnifierWorkersCount() != null) {
            return configuration.getLaEventsHandlerConfig().getUnifierWorkersCount();
        }
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        return (availableProcessors - 1) / 3;
    }
}
