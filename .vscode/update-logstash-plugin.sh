#!/bin/bash

# Define paths
PLUGIN_SRC_PATH="../src/main/java/org/logstashplugins"
PLUGIN_DEST_PATH="/usr/share/logstash/plugins/my-plugin"
LOGSTASH_PID_FILE="/var/run/logstash.pid"

# Step 1: Stop Logstash if it's running
if [ -f "$LOGSTASH_PID_FILE" ]; then
    echo "Stopping Logstash..."
    kill "$(cat $LOGSTASH_PID_FILE)"
    rm -f "$LOGSTASH_PID_FILE"
    echo "Logstash stopped."
else
    echo "Logstash is not running."
fi

# Step 2: Copy the plugin code
echo "Copying plugin code..."
cp -r "$PLUGIN_SRC_PATH" "$PLUGIN_DEST_PATH"

# Step 3: Build the plugin
echo "Building the plugin..."
cd "$PLUGIN_DEST_PATH" && ./gradlew gem -DLOGSTASH_CORE_PATH=/usr/share/logstash

# Step 4: Register the plugin with Logstash
echo "Registering the plugin with Logstash..."
/usr/share/logstash/bin/logstash-plugin install --no-verify --local "$PLUGIN_DEST_PATH/logstash-output-microsoft_sentinel_output-0.1.0-java.gem"

# Step 5: Start Logstash
echo "Starting Logstash..."
/usr/share/logstash/bin/logstash -f /usr/share/logstash/config/logstash.conf &
LOGSTASH_PID=$!
echo "$LOGSTASH_PID" > "$LOGSTASH_PID_FILE"
echo "Logstash started with PID $LOGSTASH_PID."

echo "Plugin updated successfully!"