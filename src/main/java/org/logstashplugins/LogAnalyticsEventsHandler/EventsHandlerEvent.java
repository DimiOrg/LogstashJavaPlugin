package org.logstashplugins.LogAnalyticsEventsHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventsHandlerEvent extends HashMap<String, Object> {
    public EventsHandlerEvent(Map<String, Object> originalEventData, List<String> keysToKeep) {
        for (String key : keysToKeep) {
            this.put(key, originalEventData.get(key));
        }        
    }

    public Object getLog() {
        return this;
    }
}