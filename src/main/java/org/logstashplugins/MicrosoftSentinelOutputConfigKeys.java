package org.logstashplugins;

import java.util.Collection;
import java.util.List;

import co.elastic.logstash.api.PluginConfigSpec;

public class MicrosoftSentinelOutputConfigKeys {

        // Worker configuration
        public static final PluginConfigSpec<Long> BATCHER_WORKERS_COUNT_CONFIG = 
                PluginConfigSpec.numSetting("batcher_workers_count");
        public static final PluginConfigSpec<Long> UNIFIER_WORKERS_COUNT_CONFIG = 
                PluginConfigSpec.numSetting("unifier_workers_count");
        public static final PluginConfigSpec<Long> SENDER_WORKERS_COUNT_CONFIG = 
                PluginConfigSpec.numSetting("sender_workers_count");
        // configuration specifying the delay in milliseconds after each worker iteration to prevent busy waiting
        public static final PluginConfigSpec<Long> WORKER_SLEEP_TIME_MILLIS_CONFIG = 
                PluginConfigSpec.numSetting("worker_sleep_time_millis", 10);
        public static final PluginConfigSpec<Long> MAX_RETRIES_NUM_CONFIG =
                PluginConfigSpec.numSetting("max_retries_num", 3);
        public static final PluginConfigSpec<Long> INITIAL_WAIT_TIME_SECONDS_CONFIG =
                PluginConfigSpec.numSetting("initial_wait_time_seconds", 1);
        public static final PluginConfigSpec<Long> MAX_GRACEFUL_SHUTDOWN_TIME_SECONDS_CONFIG =
                PluginConfigSpec.numSetting("max_graceful_shutdown_time_seconds", 60);
        public static final PluginConfigSpec<Long> MAX_WAITING_FOR_UNIFIER_TIME_SECONDS_CONFG = 
                PluginConfigSpec.numSetting("max_waiting_for_unifier_time_seconds", 10);
        public static final PluginConfigSpec<Long> MAX_WAITING_TIME_FOR_BATCH_SECONDS_CONFIG =
                PluginConfigSpec.numSetting("max_waiting_time_for_batch_seconds", 10);
                
        // Log Analytics configuration
        public static final PluginConfigSpec<String> DATA_COLLECTION_ENDPOINT_CONFIG = 
                PluginConfigSpec.stringSetting("data_collection_endpoint", "");
        public static final PluginConfigSpec<String> DCR_ID_CONFIG =
                PluginConfigSpec.stringSetting("dcr_id", "");
        public static final PluginConfigSpec<String> STREAM_NAME_CONFIG =
                PluginConfigSpec.stringSetting("stream_name", "");
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

        public static Collection<PluginConfigSpec<?>> getKeysCollection() {
        return List.of(
                DATA_COLLECTION_ENDPOINT_CONFIG,
                DCR_ID_CONFIG,
                STREAM_NAME_CONFIG,
                KEYS_TO_KEEP_CONFIG,
                AUTHENTICATION_TYPE_CONFIG,
                CLIENT_ID_CONFIG,
                CLIENT_SECRET_CONFIG,
                TENANT_ID_CONFIG,
                MAX_WAITING_TIME_FOR_BATCH_SECONDS_CONFIG,
                MAX_GRACEFUL_SHUTDOWN_TIME_SECONDS_CONFIG,
                MAX_WAITING_FOR_UNIFIER_TIME_SECONDS_CONFG,
                BATCHER_WORKERS_COUNT_CONFIG,
                UNIFIER_WORKERS_COUNT_CONFIG,
                SENDER_WORKERS_COUNT_CONFIG,
                WORKER_SLEEP_TIME_MILLIS_CONFIG,
                MAX_RETRIES_NUM_CONFIG,
                INITIAL_WAIT_TIME_SECONDS_CONFIG
        );
        }
    
}
