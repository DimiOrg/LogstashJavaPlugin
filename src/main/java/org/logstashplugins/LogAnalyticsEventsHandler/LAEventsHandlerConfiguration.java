package org.logstashplugins.LogAnalyticsEventsHandler;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LAEventsHandlerConfiguration {
    // Log Analytics
    private int maxWaitingTimeSeconds;
    private String dataCollectionEndpoint;
    private String dcrId;
    private String streamName;

    // Azure authentication
    private String authenticationType;
    private String clientId;
    private String clientSecret;
    private String tenantId;

    // Shutdown configuration
    private int maxGracefulShutdownTimeSeconds;
}
