package org.logstashplugins.LogAnalyticsEventsHandler;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LAEventsHandlerConfiguration {
    private SenderWorker senderWorker = new SenderWorker();
    private BatcherWorker batcherWorker = new BatcherWorker();
    private LAEventsHandler laEventsHandler = new LAEventsHandler();

    @Getter
    @Setter
    public static class SenderWorker {
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
    public static class BatcherWorker {
        private int maxWaitingTimeSecondsForBatch;
    }
    
    @Getter
    @Setter
    public static class LAEventsHandler {
        private int maxGracefulShutdownTimeSeconds;
    }
}
