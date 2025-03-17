package org.logstashplugins.LogAnalyticsEventsHandler;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LAEventsHandlerConfiguration {
    // SenderWorker
    private String dataCollectionEndpoint;
    private String dcrId;
    private String streamName;
    private int maxRetriesNum;
    private int initialWaitTimeSeconds;

    // BatcherWorker
    private int maxWaitingTimeSecondsForBatch;

    // Azure authentication
    private String authenticationType;
    private String clientId;
    private String clientSecret;
    private String tenantId;

    // Shutdown configuration
    private int maxGracefulShutdownTimeSeconds;
}
