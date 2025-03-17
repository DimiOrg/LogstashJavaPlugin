package org.logstashplugins.LogAnalyticsEventsHandler;

import java.util.Map;

public interface LAEventsHandlerEvent extends Map<String, Object> {
    public Object getLog();
}
