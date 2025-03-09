LOGSTASH_PID_FILE="/var/run/logstash.pid"
echo "Starting Logstash..."
/usr/share/logstash/bin/logstash -f /usr/share/logstash/config/logstash.conf &
LOGSTASH_PID=$!
echo "$LOGSTASH_PID" > "$LOGSTASH_PID_FILE"
echo "Logstash started with PID $LOGSTASH_PID."