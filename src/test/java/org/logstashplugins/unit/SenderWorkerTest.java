package org.logstashplugins.unit;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import com.azure.monitor.ingestion.LogsIngestionClient;
import com.azure.monitor.ingestion.models.LogsUploadException;
import org.logstashplugins.LogAnalyticsEventsHandler.LAEventsHandlerConfiguration;
import org.logstashplugins.LogAnalyticsEventsHandler.Workers.SenderWorker;

@ExtendWith(MockitoExtension.class)
public class SenderWorkerTest {

    @Mock
    private LogsIngestionClient client;

    private BlockingQueue<List<Object>> batchesQueue;
    private SenderWorker senderWorker;
    private LAEventsHandlerConfiguration.SenderWorker configuration;

    @BeforeEach
    public void setUp() {
        batchesQueue = new LinkedBlockingQueue<>();
        configuration = new LAEventsHandlerConfiguration.SenderWorker();
        configuration.setDcrId("test-dcr-id");
        configuration.setStreamName("test-stream-name");
        configuration.setMaxRetriesNum(3);
        configuration.setInitialWaitTimeSeconds(1);
        configuration.setAuthenticationType("client_secret");
        configuration.setClientId("test-client-id");
        configuration.setClientSecret("test-client-secret");
        configuration.setTenantId("test-tenant-id");
        configuration.setDataCollectionEndpoint("https://test-endpoint");

        // Initialize the SenderWorker with the real configuration and mock client
        senderWorker = new SenderWorker(batchesQueue, configuration, client);
    }

    @Test
    public void testProcess() throws InterruptedException {
        List<Object> batch1 = Arrays.asList(new Object());
        List<Object> batch2 = Arrays.asList(new Object());
        batchesQueue.add(batch1);
        batchesQueue.add(batch2);

        // Mock the client to succeed on both uploads
        doNothing().when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        // Call the process method
        senderWorker.process();

        // The client should be called twice for both batches
        verify(client, times(2)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
    }

    @Test
    public void testShutdown() throws Exception {
        List<Object> batch1 = Arrays.asList(new Object());
        List<Object> batch2 = Arrays.asList(new Object());
        batchesQueue.add(batch1);
        batchesQueue.add(batch2);
    
        // Mock the client to succeed on both uploads
        doNothing().when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
    
        // Call the shutdown method
        senderWorker.shutdown();
    
        // Verify that the client was called for each batch in the queue
        verify(client, times(2)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
    
        // Verify that isRunning() returns false after shutdown
        assertFalse(senderWorker.isRunning());
    }

    @Test
    public void testUploadWithExpBackoffRetries_RetryableException() throws Exception {
        List<Object> batch = Arrays.asList(new Object());
        batchesQueue.add(batch);

        // create a LogsUploadException mock that has a retryable exceptions
        HttpResponseException httpException = mock(HttpResponseException.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(500);
        when(httpException.getResponse()).thenReturn(httpResponse);
        LogsUploadException logsUploadException = mock(LogsUploadException.class);
        when(logsUploadException.getLogsUploadErrors()).thenReturn(Arrays.asList(httpException));

        // mock the client to throw the exception 2 times and then succeed.
        doThrow(logsUploadException).doThrow(logsUploadException).doNothing().when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        // call the private method uploadWithExpBackoffRetries using reflection
        Method method = SenderWorker.class.getDeclaredMethod("uploadWithExpBackoffRetries", List.class, int.class, long.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(senderWorker, batch, 3, 1);

        // since the exception is retryable, 2 retries should be done. the client should be called 3 times and the method should return true
        verify(client, times(3)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
        assertTrue(result);
    }

    @Test
    public void testUploadWithExpBackoffRetries_NonRetryableException() throws Exception {
        List<Object> batch = Arrays.asList(new Object());
        batchesQueue.add(batch);

        //create a LogsUploadException mock that has one retryable exception and one non-retryable exception
        LogsUploadException logsUploadException = mock(LogsUploadException.class);
        HttpResponseException retryableHttpException = mock(HttpResponseException.class);
        HttpResponseException nonRetryableHttpException = mock(HttpResponseException.class);
        HttpResponse retryableHttpResponse = mock(HttpResponse.class);
        HttpResponse nonRetryableHttpResponse = mock(HttpResponse.class);
        when(retryableHttpResponse.getStatusCode()).thenReturn(500);
        when(nonRetryableHttpResponse.getStatusCode()).thenReturn(400);
        when(retryableHttpException.getResponse()).thenReturn(retryableHttpResponse);
        when(nonRetryableHttpException.getResponse()).thenReturn(nonRetryableHttpResponse);
        when(logsUploadException.getLogsUploadErrors()).thenReturn(Arrays.asList(retryableHttpException, nonRetryableHttpException));

        // mock the client to throw the exception 1 time
        doThrow(logsUploadException).when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        // call the private method uploadWithExpBackoffRetries using reflection
        Method method = SenderWorker.class.getDeclaredMethod("uploadWithExpBackoffRetries", List.class, int.class, long.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(senderWorker, batch, 3, 1);

        // since the exception is non-retryable, no retries should be done. the client should be called only once and the method should return false
        verify(client, times(1)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
        assertFalse(result);
    }

    @Test
    public void testUploadWithExpBackoffRetries_SuccessfulUpload() throws Exception {
        List<Object> batch = Arrays.asList(new Object());
        batchesQueue.add(batch);

        // Mock the client to succeed on the first attempt
        doNothing().when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        // Call the private method uploadWithExpBackoffRetries using reflection
        Method method = SenderWorker.class.getDeclaredMethod("uploadWithExpBackoffRetries", List.class, int.class, long.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(senderWorker, batch, 3, 1);

        // The client should be called only once and the method should return true
        verify(client, times(1)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
        assertTrue(result);
    }

    @Test
    public void testUploadWithExpBackoffRetries_InterruptedException() throws Exception {
        List<Object> batch = Arrays.asList(new Object());
        batchesQueue.add(batch);

        // Create a LogsUploadException mock that has a retryable exception
        HttpResponseException httpException = mock(HttpResponseException.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(500);
        when(httpException.getResponse()).thenReturn(httpResponse);
        LogsUploadException logsUploadException = mock(LogsUploadException.class);
        when(logsUploadException.getLogsUploadErrors()).thenReturn(Arrays.asList(httpException));

        // Mock the client to throw the exception
        doThrow(logsUploadException).when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        // Interrupt the thread during the sleep
        Thread.currentThread().interrupt();

        // Call the private method uploadWithExpBackoffRetries using reflection
        Method method = SenderWorker.class.getDeclaredMethod("uploadWithExpBackoffRetries", List.class, int.class, long.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(senderWorker, batch, 3, 1);

        // The client should be called only once and the method should return false due to interruption
        verify(client, times(1)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
        assertFalse(result);
    }
}