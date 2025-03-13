package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.Output;
import co.elastic.logstash.api.PluginConfigSpec;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandler;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;

// class name must match plugin name
@LogstashPlugin(name = "microsoft_sentinel_output")
public class MicrosoftSentinelOutput implements Output {

    public static final PluginConfigSpec<String> PREFIX_CONFIG =
            PluginConfigSpec.stringSetting("prefix", "");

    // Log Analytics configuration
    public static final PluginConfigSpec<String> DATA_COLLECTION_ENDPOINT_CONFIG = 
            PluginConfigSpec.stringSetting("data_collection_endpoint", "");
    public static final PluginConfigSpec<String> DCR_ID_CONFIG =
            PluginConfigSpec.stringSetting("dcr_id", "");
    public static final PluginConfigSpec<String> STREAM_NAME_CONFIG =
            PluginConfigSpec.stringSetting("stream_name", "");
    public static final PluginConfigSpec<Long> MAX_WAITING_TIME_SECONDS_CONFIG =
            PluginConfigSpec.numSetting("max_waiting_time_seconds", 10);
    public static final PluginConfigSpec<List<Object>> KEYS_TO_KEEP_CONFIG =
            PluginConfigSpec.arraySetting("keys_to_keep");
    
    // Azure authentication
    public static final PluginConfigSpec<String> AUTHENTICATION_TYPE_CONFIG =
            PluginConfigSpec.stringSetting("authentication_type", "");
    public static final PluginConfigSpec<String> CLIENT_ID_CONFIG =
            PluginConfigSpec.stringSetting("client_id", "");
    public static final PluginConfigSpec<String> CLIENT_SECRET_CONFIG =
            PluginConfigSpec.stringSetting("client_secret", "");
    public static final PluginConfigSpec<String> TENANT_ID_CONFIG =
            PluginConfigSpec.stringSetting("tenant_id", "");


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

        LAEventsHandlerConfiguration eventsHandlerConfiguration = createEventsHandlerConfiguration(config);
        keysToKeep = (List<String>) (List<?>) config.get(KEYS_TO_KEEP_CONFIG);

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
        // set all configuration options
        eventsHandlerConfiguration.setDataCollectionEndpoint(config.get(DATA_COLLECTION_ENDPOINT_CONFIG));
        eventsHandlerConfiguration.setDcrId(config.get(DCR_ID_CONFIG));
        eventsHandlerConfiguration.setStreamName(config.get(STREAM_NAME_CONFIG));
        eventsHandlerConfiguration.setMaxWaitingTimeSeconds(config.get(MAX_WAITING_TIME_SECONDS_CONFIG).intValue());
        eventsHandlerConfiguration.setAuthenticationType(config.get(AUTHENTICATION_TYPE_CONFIG));
        eventsHandlerConfiguration.setClientId(config.get(CLIENT_ID_CONFIG));   
        eventsHandlerConfiguration.setClientSecret(config.get(CLIENT_SECRET_CONFIG));
        eventsHandlerConfiguration.setTenantId(config.get(TENANT_ID_CONFIG));

        return eventsHandlerConfiguration;
    }

    @Override
    public void stop() {
        stopped = true;
        eventsHandler.shutdown();
        done.countDown();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        return List.of(
            DATA_COLLECTION_ENDPOINT_CONFIG,
            DCR_ID_CONFIG,
            STREAM_NAME_CONFIG,
            MAX_WAITING_TIME_SECONDS_CONFIG,
            KEYS_TO_KEEP_CONFIG,
            AUTHENTICATION_TYPE_CONFIG,
            CLIENT_ID_CONFIG,
            CLIENT_SECRET_CONFIG,
            TENANT_ID_CONFIG
        );
    }

    @Override
    public String getId() {
        return id;
    }
}
