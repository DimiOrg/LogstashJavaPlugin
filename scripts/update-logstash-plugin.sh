#!/bin/bash

# Step 1: Stop Logstash if it's running
LOGSTASH_PID=$(ps aux | grep "/usr/share/logstash/jdk/bin/java" | grep -v grep | awk '{print $2}')

if [ -z "$LOGSTASH_PID" ]; then
    echo "Logstash is not running."
else
    echo "Stopping Logstash... (PID: $LOGSTASH_PID)"
    kill -9 "$LOGSTASH_PID"
    echo "Logstash stopped."
fi

# Step 2: Copy the updated plugin code and logstash configuration
echo "Copying updated plugin code..."
cp -r src/main/java/org/logstashplugins /usr/share/logstash/plugins/my-plugin
cp envsInstallationResources/Logstash8.16/logstash.conf /usr/share/logstash/config/logstash.conf

# Step 3: Build the plugin
echo "Building the plugin..."
cd /usr/share/logstash/plugins/my-plugin && ./gradlew gem -DLOGSTASH_CORE_PATH=/usr/share/logstash

# Step 4: Register the plugin with Logstash
echo "Registering the plugin with Logstash..."
/usr/share/logstash/bin/logstash-plugin install --no-verify --local /usr/share/logstash/plugins/my-plugin/logstash-output-microsoft_sentinel_output-0.1.0-java.gem

# Step 5: Start Logstash
echo "Starting Logstash..."
/usr/share/logstash/bin/logstash -f /usr/share/logstash/config/logstash.conf