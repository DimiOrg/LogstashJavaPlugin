package org.logstashplugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;

public class LogstashLAHandlerEvent extends HashMap<String, Object> implements LAEventsHandlerEvent {
    public LogstashLAHandlerEvent(Map<String, Object> originalEventData, List<String> keysToKeep) {
        for (String key : keysToKeep) {
            this.put(key, originalEventData.get(key));
        }        
    }

    @Override
    public Object getLog() {
        return this;
    }
}