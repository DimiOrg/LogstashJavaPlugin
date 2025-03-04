package org.logstashplugins.LogAnalyticsEventsHandler;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;

public class TokenCredentialFactory {
    public static TokenCredential createCredential(String authenticationType, String clientId, String clientSecret, String tenantId) {
        if ("secret".equalsIgnoreCase(authenticationType)) {
            return new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
        } else if ("managed".equalsIgnoreCase(authenticationType)) {
            return new DefaultAzureCredentialBuilder().build();
        } else {
            throw new IllegalArgumentException("Invalid authentication type: " + authenticationType);
        }
    }
}
