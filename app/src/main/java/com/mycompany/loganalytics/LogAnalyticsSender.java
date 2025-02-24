import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.ingestion.LogsIngestionClient;
import com.azure.monitor.ingestion.LogsIngestionClientBuilder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LogAnalyticsSender {

    // Replace these values with your actual Azure settings
    private static final String DCR_IMMUTABLE_ID = "DCR_IMMUTABLE_ID"; 
    private static final String DCE_ENDPOINT = "DCE_ENDPOINT"; 
    private static final String STREAM_NAME = "STREAM_NAME"; 

    public static void main(String[] args) {
        try {
            // Create an Azure Monitor Ingestion client
            LogsIngestionClient client = new LogsIngestionClientBuilder()
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .endpoint(DCE_ENDPOINT)
                    .buildClient();

            // Prepare log data (must match schema). This is a sample
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("TimeGenerated", "2025-02-23T12:34:56Z");
            logEntry.put("Message", "This is a test log from Java app");
            logEntry.put("LogLevel", "INFO");
            logEntry.put("User", "TestUser");

            // Convert to Iterable<Object> (fixes the compilation error)
            List<Object> logs = new ArrayList<>();
            logs.add(logEntry);  

            // Print JSON payload for debugging
            System.out.println("JSON Payload:");
            System.out.println(logEntry);

            // Send logs using Azure Monitor Ingestion SDK
            client.upload(DCR_IMMUTABLE_ID, STREAM_NAME, logs);

            System.out.println("Logs sent successfully!");

        } 
        catch (com.azure.monitor.ingestion.models.LogsUploadException logsException) {
            System.out.println("Azure Logs Upload Error: " + logsException.getMessage());

            // Print full stack trace for debugging
            logsException.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("General Error: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
