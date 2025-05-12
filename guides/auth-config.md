## Authentication Type

The output plugin supports two authentication methods for securely sending logs to Microsoft Sentinel via Azure Monitor:

- **`secret`** – Client ID + Secret–based authentication  
- **`managed`** – Azure Managed Identity authentication

Set the desired method using the `authentication_type` parameter in your `logstash.conf` output block.

### Secret-Based Authentication

Use this method when authenticating with an Azure AD application (client ID and client secret).

**Example configuration:**

```logstash
output {
  microsoft_sentinel_output {
    data_collection_endpoint => "https://<your-dce-ingestion-endpoint>"
    dcr_id => "dcr-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
    stream_name => "Custom-logstash_CL"
    authentication_type => "secret"
    tenant_id => "<your-tenant-id>"
    client_id => "<your-app-client-id>"
    client_secret => "<your-app-client-secret>"
  }
}
```

### Managed Identity Authentication (Azure VM)

Use this method when authenticating with Azure Managed Identity enabled.

**Example configuration:**

```logstash
output {
  microsoft_sentinel_output {
    data_collection_endpoint => "https://<your-dce-ingestion-endpoint>"
    dcr_id => "dcr-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
    stream_name => "Custom-logstash_CL"
    authentication_type => "managed"
  }
}
```

### Required Azure Permissions

The identity (either a service principal or a VM's system-assigned managed identity) must be granted the following role:

- **Role**: Monitoring Metrics Publisher  
- **Scope**: The target Data Collection Rule (DCR) + Data Collection Endpoint (DCE)

**Assigning the role via Azure CLI:**

```bash
az role assignment create \
  --assignee <object-id-of-identity> \
  --role "Monitoring Metrics Publisher" \
  --scope "/subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.Insights/dataCollectionRules/<dcr-name>"
```

### Network Requirements

When using Managed Identity, the plugin fetches an Azure access token from the Azure Instance Metadata Service (IMDS):

- **URL**: `http://169.254.169.254/metadata/identity/oauth2/token`

This endpoint must be accessible from the container or VM. It is available by default on Azure VMs.

#### Token Retrieval Check (HTTP 200 Required)

Before testing log ingestion, ensure the plugin (or environment) can successfully retrieve a Managed Identity token.

**Manual check:**

```bash
curl -s -o /dev/null -w "%{http_code}\n" -H Metadata:true \
  "http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&resource=https://monitor.azure.com"
```

### How to Verify Log Ingestion

1. **Fetch an access token manually (for Managed Identity):**

```bash
curl -H Metadata:true \
  "http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&resource=https://monitor.azure.com"
```

Check that the response includes a valid `access_token`.

2. **Send a manual test log:**

```bash
curl -X POST \
  "https://<your-dce-ingestion-endpoint>/dataCollectionRules/<dcr-id>/streams/<stream-name>?api-version=2023-01-01" \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "message": "Test log from curl",
      "ls_version": "8.16.0",
      "host": "test-host",
      "sequence": 1,
      "ls_timestamp": "2024-05-08T10:40:00Z",
      "logstash_version": "2024-05-08T10:40:00Z"
    }
  ]'
```

**A successful response will return:**

```http
HTTP/1.1 204 No Content
```
