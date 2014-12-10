package org.openrepose.core.services.httpclient;

import org.apache.http.client.HttpClient;

//todo: change the name of this using response mixes contexts
public interface HttpClientResponse {
    HttpClient getHttpClient();
    String getClientInstanceId();
    String getUserId();
}
