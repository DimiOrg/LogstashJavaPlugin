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

import org.logstashplugins.LogAnalyticsEventsHandler.EventsHandler;
import org.logstashplugins.LogAnalyticsEventsHandler.EventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.EventsHandlerEvent;

// class name must match plugin name
@LogstashPlugin(name = "microsoft_sentinel_output")
public class MicrosoftSentinelOutput implements Output {

    public static final PluginConfigSpec<String> PREFIX_CONFIG =
            PluginConfigSpec.stringSetting("prefix", "");
    public static final PluginConfigSpec<String> DATA_COLLECTION_ENDPOINT_CONFIG = 
            PluginConfigSpec.stringSetting("data_collection_endpoint", "");
    public static final PluginConfigSpec<String> DCR_ID_CONFIG =
            PluginConfigSpec.stringSetting("dcr_id", "");
    public static final PluginConfigSpec<String> TABLE_NAME_CONFIG =
            PluginConfigSpec.stringSetting("table_name", "");
    public static final PluginConfigSpec<Long> MAX_WAITING_TIME_SECONDS_CONFIG =
            PluginConfigSpec.numSetting("max_waiting_time_seconds", 10);
    public static final PluginConfigSpec<List<Object>> KEYS_TO_KEEP_CONFIG =
            PluginConfigSpec.arraySetting("keys_to_keep");


    private final String id;
    private String prefix;
    private PrintStream printer;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;

    private EventsHandler eventsHandler;
    private List<String> keysToKeep;

    // all plugins must provide a constructor that accepts id, Configuration, and Context
    public MicrosoftSentinelOutput(final String id, final Configuration configuration, final Context context) {
        this(id, configuration, context, System.out);
    }

    @SuppressWarnings("unchecked")
    public MicrosoftSentinelOutput(final String id, final Configuration config, final Context context, OutputStream targetStream) {
        // constructors should validate configuration options
        this.id = id;
        prefix = config.get(PREFIX_CONFIG);
        printer = new PrintStream(targetStream);

        EventsHandlerConfiguration eventsHandlerConfiguration = createEventsHandlerConfiguration(config);
        keysToKeep = (List<String>) (List<?>) config.get(KEYS_TO_KEEP_CONFIG);

        eventsHandler = new EventsHandler(eventsHandlerConfiguration);
    }

    @Override
    public void output(final Collection<Event> events) {
        Iterator<Event> z = events.iterator();
        while (z.hasNext() && !stopped) {
            EventsHandlerEvent event = new EventsHandlerEvent(z.next().getData(), keysToKeep);
            eventsHandler.handle(event);
        }
    }

    private EventsHandlerConfiguration createEventsHandlerConfiguration(Configuration config) {
        EventsHandlerConfiguration eventsHandlerConfiguration = new EventsHandlerConfiguration();
        // set all configuration options
        eventsHandlerConfiguration.setDataCollectionEndpoint(config.get(DATA_COLLECTION_ENDPOINT_CONFIG));
        eventsHandlerConfiguration.setDcrId(config.get(DCR_ID_CONFIG));
        eventsHandlerConfiguration.setTableName(config.get(TABLE_NAME_CONFIG));
        eventsHandlerConfiguration.setMaxWaitingTimeSeconds(config.get(MAX_WAITING_TIME_SECONDS_CONFIG).intValue());

        return eventsHandlerConfiguration;
    }

    @Override
    public void stop() {
        stopped = true;
        done.countDown();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Collections.singletonList(PREFIX_CONFIG);
    }

    @Override
    public String getId() {
        return id;
    }
}
