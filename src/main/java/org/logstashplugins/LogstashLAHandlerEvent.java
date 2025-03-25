package org.logstashplugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerEvent;

public class LogstashLAHandlerEvent extends HashMap<String, Object> implements LAEventsHandlerEvent {
    private static final Map<String, String> keysToConvert = new HashMap<String, String>() {
        {
            put("@version", "ls_version");
            put("@timestamp", "ls_timestamp");
        }
    };

    private static final String HOST_TARGET_KEY = "host";
    private static final String EVENT_KEY = "event";
    private static final String SEQUENCE_KEY = "sequence";

    public LogstashLAHandlerEvent(Map<String, Object> eventDataAsIdentityMap, List<String> keysToKeep) {
        List<String> keysToRemove = new ArrayList<>();
        Map<String, Object> keysToUpdate = new HashMap<>();

        for (Map.Entry<String, Object> entry : eventDataAsIdentityMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (keysToConvert.containsKey(key)) {
                // convert keys containing @ to ls_. this is needed because @ is not allowed in kql
                keysToUpdate.put(keysToConvert.get(key), value);
                keysToRemove.add(key);
            } else if (key.equals(HOST_TARGET_KEY)) {
                // flatten host key
                Map<String, Object> host = (Map<String, Object>) value;
                if (host.containsKey("name")) {
                    keysToUpdate.put(HOST_TARGET_KEY, host.get("name"));
                } else if (host.containsKey("hostname")) {
                    keysToUpdate.put(HOST_TARGET_KEY, host.get("hostname"));
                } else {
                    keysToUpdate.put(HOST_TARGET_KEY, host);
                }
                keysToRemove.add(key);
            } else if (key.equals(EVENT_KEY)) {
                // flatten event key
                Map<String, Object> event = (Map<String, Object>) value;
                keysToUpdate.put(SEQUENCE_KEY, event.get("sequence"));
                keysToRemove.add(key);
            } else {
                // if keysToKeep is empty, keep all keys. otherwise, only keep the keys in keysToKeep
                if (keysToKeep == null || keysToKeep.isEmpty() || keysToKeep.contains(key)) {
                    this.put(key, value);
                } else {
                    keysToRemove.add(key);
                }
            }
        }

        // Remove keys marked for removal
        for (String key : keysToRemove) {
            eventDataAsIdentityMap.remove(key);
        }

        // Update the map with new keys
        eventDataAsIdentityMap.putAll(keysToUpdate);

        // Update this object with the new map
        this.putAll(eventDataAsIdentityMap);
    }

    @Override
    public Object getLog() {
        return this;
    }
}