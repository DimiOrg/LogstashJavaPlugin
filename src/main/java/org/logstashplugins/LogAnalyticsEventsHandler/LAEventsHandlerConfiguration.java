package org.logstashplugins.LogAnalyticsEventsHandler;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LAEventsHandlerConfiguration {
    private SenderWorkerConfig senderWorkerConfig = new SenderWorkerConfig();
    private BatcherWorkerConfig batcherWorkerConfig = new BatcherWorkerConfig();
    private UnifierWorkerConfig unifierWorkerConfig = new UnifierWorkerConfig();
    private LAEventsHandlerConfig laEventsHandlerConfig = new LAEventsHandlerConfig();

    @Getter
    @Setter
    public static class SenderWorkerConfig {
        private String dataCollectionEndpoint;
        private String dcrId;
        private String streamName;
        private int maxRetriesNum;
        private int initialWaitTimeSeconds;    
        private String authenticationType;
        private String clientId;
        private String clientSecret;
        private String tenantId;
    }
    
    @Getter
    @Setter
    public static class BatcherWorkerConfig {
        private int maxWaitingTimeSecondsForBatch;
    }
    
    @Getter
    @Setter
    public static class LAEventsHandlerConfig {
        private int maxGracefulShutdownTimeSeconds;
    }

    @Getter
    @Setter
    public static class UnifierWorkerConfig {
        private int maxWaitingForUnifierTimeSeconds;
    }
}
