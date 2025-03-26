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

# Step 2: Copy the updated plugin code and logstash configuration and gradle files
echo "Copying updated plugin code and configuration..."
rm -rf /usr/share/logstash/plugins/my-plugin
mkdir -p /usr/share/logstash/plugins/my-plugin/src/main/java/org/logstashplugins
cp -r src/main/java/org/logstashplugins /usr/share/logstash/plugins/my-plugin/src/main/java/org/logstashplugins
cp envsInstallationResources/Logstash8.16/build.gradle /usr/share/logstash/plugins/my-plugin/build.gradle
cp envsInstallationResources/Logstash8.16/rubyUtils.gradle /usr/share/logstash/plugins/my-plugin/rubyUtils.gradle
cp envsInstallationResources/Logstash8.16/settings.gradle /usr/share/logstash/plugins/my-plugin/settings.gradle
cp envsInstallationResources/Logstash8.16/logstash.conf /usr/share/logstash/config/logstash.conf

# Download the versions.yml file
wget -O /usr/share/logstash/plugins/my-plugin/versions.yml https://raw.githubusercontent.com/elastic/logstash/main/versions.yml

# Create the VERSION file for the plugin
echo "0.1.0" > /usr/share/logstash/plugins/my-plugin/VERSION
# Step 3: Build the plugin
echo "Building the plugin..."
cd /usr/share/logstash/plugins/my-plugin
gradle wrapper
chmod +x /usr/share/logstash/plugins/my-plugin/gradlew
# Set the LOGSTASH_CORE_PATH environment variable for Gradle
echo "org.gradle.jvmargs=-DLOGSTASH_CORE_PATH=${LOGSTASH_CORE_PATH}" >> /usr/share/logstash/plugins/my-plugin/gradle.properties

# Clean previous build artifacts
./gradlew clean

# Build the gem
./gradlew gem -DLOGSTASH_CORE_PATH=${LOGSTASH_CORE_PATH}

# Step 4: Register the plugin with Logstash
echo "Registering the plugin with Logstash..."
/usr/share/logstash/bin/logstash-plugin install --no-verify --local /usr/share/logstash/plugins/my-plugin/logstash-output-microsoft_sentinel_output-0.1.0-java.gem

# Step 5: Start Logstash
echo "Starting Logstash..."
/usr/share/logstash/bin/logstash -f /usr/share/logstash/config/logstash.conf