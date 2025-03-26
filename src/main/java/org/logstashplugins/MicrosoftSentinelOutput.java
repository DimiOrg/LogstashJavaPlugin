package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.Output;
import co.elastic.logstash.api.PluginConfigSpec;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandler;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// class name must match plugin name
@LogstashPlugin(name = "microsoft_sentinel_output")
public class MicrosoftSentinelOutput implements Output {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftSentinelOutput.class);

    private final String id;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;

    private LAEventsHandler eventsHandler;
    private List<String> keysToKeep;

    // all plugins must provide a constructor that accepts id, Configuration, and Context
    public MicrosoftSentinelOutput(final String id, final Configuration configuration, final Context context) {
        this(id, configuration, context, System.out);
    }

    @SuppressWarnings("unchecked")
    public MicrosoftSentinelOutput(final String id, final Configuration config, final Context context, OutputStream targetStream) {
        // constructors should validate configuration options
        this.id = id;

        logger.info("Starting Microsoft Sentinel output plugin");
        LAEventsHandlerConfiguration eventsHandlerConfiguration = createEventsHandlerConfiguration(config);
        keysToKeep = (List<String>) (List<?>) config.get(MicrosoftSentinelOutputConfigKeys.KEYS_TO_KEEP_CONFIG);

        eventsHandler = new LAEventsHandler(eventsHandlerConfiguration);
    }

    @Override
    public void output(final Collection<Event> events) {
        Iterator<Event> z = events.iterator();
        while (z.hasNext() && !stopped) {
            LAEventsHandlerEvent event = new LogstashLAHandlerEvent(z.next().getData(), keysToKeep);
            eventsHandler.handle(event);
        }
    }

    private LAEventsHandlerConfiguration createEventsHandlerConfiguration(Configuration config) {
        LAEventsHandlerConfiguration eventsHandlerConfiguration = new LAEventsHandlerConfiguration();

        // Log Analytics configuration
        eventsHandlerConfiguration.getSenderWorkerConfig().setDataCollectionEndpoint(config.get(MicrosoftSentinelOutputConfigKeys.DATA_COLLECTION_ENDPOINT_CONFIG));
        eventsHandlerConfiguration.getSenderWorkerConfig().setDcrId(config.get(MicrosoftSentinelOutputConfigKeys.DCR_ID_CONFIG));
        eventsHandlerConfiguration.getSenderWorkerConfig().setStreamName(config.get(MicrosoftSentinelOutputConfigKeys.STREAM_NAME_CONFIG));

        // Azure authentication
        eventsHandlerConfiguration.getSenderWorkerConfig().setAuthenticationType(config.get(MicrosoftSentinelOutputConfigKeys.AUTHENTICATION_TYPE_CONFIG));
        eventsHandlerConfiguration.getSenderWorkerConfig().setClientId(config.get(MicrosoftSentinelOutputConfigKeys.CLIENT_ID_CONFIG));   
        eventsHandlerConfiguration.getSenderWorkerConfig().setClientSecret(config.get(MicrosoftSentinelOutputConfigKeys.CLIENT_SECRET_CONFIG));
        eventsHandlerConfiguration.getSenderWorkerConfig().setTenantId(config.get(MicrosoftSentinelOutputConfigKeys.TENANT_ID_CONFIG));

        // Worker configuration
        eventsHandlerConfiguration.getSenderWorkerConfig().setMaxRetriesNum(config.get(MicrosoftSentinelOutputConfigKeys.MAX_RETRIES_NUM_CONFIG).intValue());
        eventsHandlerConfiguration.getSenderWorkerConfig().setInitialWaitTimeSeconds(config.get(MicrosoftSentinelOutputConfigKeys.INITIAL_WAIT_TIME_SECONDS_CONFIG).intValue());
        eventsHandlerConfiguration.getBatcherWorkerConfig().setMaxWaitingTimeSecondsForBatch(config.get(MicrosoftSentinelOutputConfigKeys.MAX_WAITING_TIME_FOR_BATCH_SECONDS_CONFIG).intValue());
        eventsHandlerConfiguration.getUnifierWorkerConfig().setMaxWaitingForUnifierTimeSeconds(config.get(MicrosoftSentinelOutputConfigKeys.MAX_WAITING_FOR_UNIFIER_TIME_SECONDS_CONFG).intValue());
        eventsHandlerConfiguration.getLaEventsHandlerConfig().setMaxGracefulShutdownTimeSeconds(config.get(MicrosoftSentinelOutputConfigKeys.MAX_GRACEFUL_SHUTDOWN_TIME_SECONDS_CONFIG).intValue());

        eventsHandlerConfiguration.getSenderWorkerConfig().setSleepTimeMillis(config.get(MicrosoftSentinelOutputConfigKeys.WORKER_SLEEP_TIME_MILLIS_CONFIG).intValue());
        eventsHandlerConfiguration.getBatcherWorkerConfig().setSleepTimeMillis(config.get(MicrosoftSentinelOutputConfigKeys.WORKER_SLEEP_TIME_MILLIS_CONFIG).intValue());
        eventsHandlerConfiguration.getUnifierWorkerConfig().setSleepTimeMillis(config.get(MicrosoftSentinelOutputConfigKeys.WORKER_SLEEP_TIME_MILLIS_CONFIG).intValue());

        Optional<Long> batcherWorkersCount = Optional.ofNullable(config.get(MicrosoftSentinelOutputConfigKeys.BATCHER_WORKERS_COUNT_CONFIG));
        batcherWorkersCount.ifPresent(value -> eventsHandlerConfiguration.getLaEventsHandlerConfig().setBatcherWorkersCount(value.intValue()));
        Optional<Long> senderWorkersCount = Optional.ofNullable(config.get(MicrosoftSentinelOutputConfigKeys.SENDER_WORKERS_COUNT_CONFIG));
        senderWorkersCount.ifPresent(value -> eventsHandlerConfiguration.getLaEventsHandlerConfig().setSenderWorkersCount(value.intValue()));
        Optional<Long> unifierWorkersCount = Optional.ofNullable(config.get(MicrosoftSentinelOutputConfigKeys.UNIFIER_WORKERS_COUNT_CONFIG));
        unifierWorkersCount.ifPresent(value -> eventsHandlerConfiguration.getLaEventsHandlerConfig().setUnifierWorkersCount(value.intValue()));

        return eventsHandlerConfiguration;
    }

    @Override
    public void stop() {
        logger.info("Stopping Microsoft Sentinel output plugin");
        stopped = true;
        eventsHandler.shutdown();
        done.countDown();
        logger.info("Microsoft Sentinel output plugin stopped");
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        return MicrosoftSentinelOutputConfigKeys.getKeysCollection();
    }

    @Override
    public String getId() {
        return id;
    }
}
