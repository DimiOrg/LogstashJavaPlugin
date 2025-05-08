package org.logstashplugins.LogAnalyticsEventsHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.identity.ClientCertificateCredentialBuilder;

public class TokenCredentialFactory {
    private static final Logger logger = LoggerFactory.getLogger(TokenCredentialFactory.class);
    public static TokenCredential createCredential(String authenticationType, String clientId, String clientSecret, String tenantId, String certificatePath) {
        logger.debug("Creating token credential with authentication type: {}", authenticationType);
        if ("secret".equalsIgnoreCase(authenticationType)) {
            return new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
        } else if ("managed".equalsIgnoreCase(authenticationType)) {
            return new DefaultAzureCredentialBuilder().build();        
        } else if ("certificate".equalsIgnoreCase(authenticationType)) {
            return new ClientCertificateCredentialBuilder()
                .clientId(clientId)
                .pemCertificate(certificatePath)
                .tenantId(tenantId)
                .build();
        } else {
            throw new IllegalArgumentException("Invalid authentication type: " + authenticationType);
        }
    }
}
