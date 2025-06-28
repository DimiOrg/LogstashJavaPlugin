# Use an official OpenJDK runtime as a parent image
FROM openjdk:11-jdk

# Set environment variables
ENV LOGSTASH_VERSION=8.16.0
ENV GRADLE_VERSION=7.3
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8
ENV LOGSTASH_CORE_PATH=/usr/share/logstash
ENV LS_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ENV PIPELINE_ECS_COMPATIBILITY=disabled

# Install dependencies
RUN apt-get update && \
    apt-get install -y gcc git locales jq make patch debianutils ruby ruby-dev build-essential openssh-server sudo lsb-release procps wget && \
    rm -rf /var/lib/apt/lists/*

# Set up locale
RUN echo "en_US.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen en_US.UTF-8 && \
    update-locale LANG=en_US.UTF-8

# Install Gradle
RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip && \
    unzip gradle-${GRADLE_VERSION}-bin.zip -d /opt && \
    ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle && \
    rm gradle-${GRADLE_VERSION}-bin.zip

ENV PATH="/opt/gradle/bin:${PATH}"

# Install Logstash
RUN wget https://artifacts.elastic.co/downloads/logstash/logstash-8.16.0-linux-x86_64.tar.gz && \
    tar -xzf logstash-8.16.0-linux-x86_64.tar.gz && \
    mv logstash-8.16.0 /usr/share/logstash && \
    rm logstash-8.16.0-linux-x86_64.tar.gz

ENV LS_HOME="/usr/share/logstash"

# Set the working directory
WORKDIR /usr/share/logstash

# Install Logstash dependencies
RUN ./bin/logstash-plugin install logstash-input-beats

# Copy the plugin code and Gradle files into the container
COPY ../src/main/java/org/logstashplugins /usr/share/logstash/plugins/my-plugin/src/main/java/org/logstashplugins
COPY ../envsInstallationResources/Logstash8.16/settings.gradle /usr/share/logstash/plugins/my-plugin/settings.gradle
COPY ../envsInstallationResources/Logstash8.16/build.gradle /usr/share/logstash/plugins/my-plugin/build.gradle
COPY ../envsInstallationResources/Logstash8.16/rubyUtils.gradle /usr/share/rubyUtils.gradle

# Copy the versions.yml files
COPY ../envsInstallationResources/Logstash8.16/versions.yml /usr/share/logstash/plugins/my-plugin/versions.yml

# Create the VERSION file for the plugin
RUN echo "0.1.0" > /usr/share/logstash/plugins/my-plugin/VERSION

# Ensure the Gradle wrapper is executable
WORKDIR /usr/share/logstash/plugins/my-plugin
RUN gradle wrapper
RUN chmod +x /usr/share/logstash/plugins/my-plugin/gradlew

# Set the LOGSTASH_CORE_PATH environment variable for Gradle
RUN echo "org.gradle.jvmargs=-DLOGSTASH_CORE_PATH=${LOGSTASH_CORE_PATH}" >> /usr/share/logstash/plugins/my-plugin/gradle.properties

# Build the plugin
RUN ./gradlew gem -DLOGSTASH_CORE_PATH=${LOGSTASH_CORE_PATH}

# List the contents of the build directory
RUN ls -l /usr/share/logstash/plugins/my-plugin/build/libs

# Register the plugin with Logstash
RUN /usr/share/logstash/bin/logstash-plugin install --no-verify --local /usr/share/logstash/plugins/my-plugin/logstash-output-microsoft_sentinel_output-0.1.0-java.gem

# Copy the Logstash configuration file
COPY ../envsInstallationResources/Logstash8.16/logstash.conf /usr/share/logstash/config/logstash.conf

# Configure SSH
RUN mkdir /var/run/sshd && \
    echo 'root:root' | chpasswd && \
    sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/UsePAM yes/UsePAM no/' /etc/ssh/sshd_config

# Expose the remote debugging port
EXPOSE 5005