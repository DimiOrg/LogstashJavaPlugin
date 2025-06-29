package org.logstashplugins.unit;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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
    private LAEventsHandlerConfiguration.SenderWorkerConfig configuration;

    @BeforeEach
    public void setUp() {
        batchesQueue = new LinkedBlockingQueue<>();
        configuration = new LAEventsHandlerConfiguration.SenderWorkerConfig();
        configuration.setDcrId("test-dcr-id");
        configuration.setStreamName("test-stream-name");
        configuration.setMaxRetriesNum(3);
        configuration.setInitialWaitTimeSeconds(1);
        configuration.setAuthenticationType("client_secret");
        configuration.setClientId("test-client-id");
        configuration.setClientSecret("test-client-secret");
        configuration.setTenantId("test-tenant-id");
        configuration.setDataCollectionEndpoint("https://test-endpoint");

        senderWorker = new SenderWorker(batchesQueue, configuration, client);
    }

    @Test
    public void testProcess() throws InterruptedException {
        List<Object> batch1 = Arrays.asList(new Object());
        List<Object> batch2 = Arrays.asList(new Object());
        batchesQueue.add(batch1);
        batchesQueue.add(batch2);

        doNothing().when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        Thread workerThread = new Thread(() -> {
            try {
                senderWorker.process();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        workerThread.start();

        Thread.sleep(1000); // allow time for processing
        senderWorker.shutdown();
        workerThread.join(2000);

        verify(client, times(2)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
    }

    @Test
    public void testShutdown() throws Exception {
        List<Object> batch1 = Arrays.asList(new Object());
        List<Object> batch2 = Arrays.asList(new Object());
        batchesQueue.add(batch1);
        batchesQueue.add(batch2);

        doNothing().when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        senderWorker.shutdown();

        verify(client, times(2)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
        assertFalse(senderWorker.isRunning());
    }

    @Test
    public void testUploadWithExpBackoffRetries_RetryableException() throws Exception {
        List<Object> batch = Arrays.asList(new Object());
        batchesQueue.add(batch);

        HttpResponseException httpException = mock(HttpResponseException.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(500);
        when(httpException.getResponse()).thenReturn(httpResponse);
        LogsUploadException logsUploadException = mock(LogsUploadException.class);
        when(logsUploadException.getLogsUploadErrors()).thenReturn(Arrays.asList(httpException));

        doThrow(logsUploadException).doThrow(logsUploadException).doNothing()
            .when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        Method method = SenderWorker.class.getDeclaredMethod("uploadWithExpBackoffRetries", List.class, int.class, long.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(senderWorker, batch, 3, 1);

        verify(client, times(3)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
        assertTrue(result);
    }

    @Test
    public void testUploadWithExpBackoffRetries_NonRetryableException() throws Exception {
        List<Object> batch = Arrays.asList(new Object());
        batchesQueue.add(batch);

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

        doThrow(logsUploadException).when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        Method method = SenderWorker.class.getDeclaredMethod("uploadWithExpBackoffRetries", List.class, int.class, long.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(senderWorker, batch, 3, 1);

        verify(client, times(1)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
        assertFalse(result);
    }

    @Test
    public void testUploadWithExpBackoffRetries_SuccessfulUpload() throws Exception {
        List<Object> batch = Arrays.asList(new Object());
        batchesQueue.add(batch);

        doNothing().when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        Method method = SenderWorker.class.getDeclaredMethod("uploadWithExpBackoffRetries", List.class, int.class, long.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(senderWorker, batch, 3, 1);

        verify(client, times(1)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
        assertTrue(result);
    }

    @Test
    public void testUploadWithExpBackoffRetries_InterruptedException() throws Exception {
        List<Object> batch = Arrays.asList(new Object());
        batchesQueue.add(batch);

        HttpResponseException httpException = mock(HttpResponseException.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(500);
        when(httpException.getResponse()).thenReturn(httpResponse);
        LogsUploadException logsUploadException = mock(LogsUploadException.class);
        when(logsUploadException.getLogsUploadErrors()).thenReturn(Arrays.asList(httpException));

        doThrow(logsUploadException).when(client).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());

        Thread.currentThread().interrupt();

        Method method = SenderWorker.class.getDeclaredMethod("uploadWithExpBackoffRetries", List.class, int.class, long.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(senderWorker, batch, 3, 1);

        verify(client, times(1)).upload(eq("test-dcr-id"), eq("test-stream-name"), anyList());
        assertFalse(result);
    }
}
