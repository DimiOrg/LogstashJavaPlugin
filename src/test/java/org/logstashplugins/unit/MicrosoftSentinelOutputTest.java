package org.logstashplugins.unit;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Event;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.logstash.plugins.ConfigurationImpl;
import org.logstashplugins.MicrosoftSentinelOutput;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MicrosoftSentinelOutputTest {

    @Disabled
    @Test
    public void testMicrosoftSentinelOutput() {
        String prefix = "Prefix";
        Map<String, Object> configValues = new HashMap<>();
        Configuration config = new ConfigurationImpl(configValues);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MicrosoftSentinelOutput output = new MicrosoftSentinelOutput("test-id", config, null, baos);

        String sourceField = "message";
        int eventCount = 5;
        Collection<Event> events = new ArrayList<>();
        for (int k = 0; k < eventCount; k++) {
            Event e = new org.logstash.Event();
            e.setField(sourceField, "message " + k);
            events.add(e);
        }

        output.output(events);

        String outputString = baos.toString();
        int index = 0;
        int lastIndex = 0;
        while (index < eventCount) {
            lastIndex = outputString.indexOf(prefix, lastIndex);
            Assertions.assertTrue(lastIndex > -1, "Prefix should exist in output string");
            lastIndex = outputString.indexOf("message " + index);
            Assertions.assertTrue(lastIndex > -1, "Message should exist in output string");
            index++;
        }
    }
}