package org.logstashplugins.LogAnalyticsEventsHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;

public class TokenCredentialFactory {
    private static final Logger logger = LoggerFactory.getLogger(TokenCredentialFactory.class);
    public static TokenCredential createCredential(String authenticationType, String clientId, String clientSecret, String tenantId, String certificatePath, String certificateType, String certificatePassword) {
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
            ClientCertificateCredentialBuilder clientCertificateCredentialBuilder = new ClientCertificateCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId);
            
            if("pfx".equalsIgnoreCase(certificateType)){
                clientCertificateCredentialBuilder.pfxCertificate(certificatePath, certificatePassword);
            } else if("pem".equalsIgnoreCase(certificateType)){
                clientCertificateCredentialBuilder.pemCertificate(certificatePath);
            } else {
                throw new IllegalArgumentException("Invalid certificate type: " + certificateType);    
            }            
            return clientCertificateCredentialBuilder.build();
        } else {
            throw new IllegalArgumentException("Invalid authentication type: " + authenticationType);
        }
    }
}
