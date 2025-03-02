package org.logstashplugins.LogAnalyticsEventsHandler;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventsHandlerConfiguration {
    private int maxWaitingTimeSeconds;
    private int maxEventsPerBatch;
    private String dataCollectionEndpoint;
    private String dcrId;
    private String tableName;
}
