package org.logstashplugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;

public class LogstashLAHandlerEvent extends HashMap<String, Object> implements LAEventsHandlerEvent {
    public LogstashLAHandlerEvent(Map<String, Object> eventDataAsIdentityMap, List<String> keysToKeep) {
        HashMap<String, Object> eventDataMap = new HashMap<String, Object>(eventDataAsIdentityMap);
        for (String key : keysToKeep) {
            this.put(key, eventDataMap.get(key));
        }        
    }

    @Override
    public Object getLog() {
        return this;
    }
}