package org.openrepose.services.httpclient.api;

import org.apache.http.client.HttpClient;

//todo: change the name of this using response mixes contexts
public interface HttpClientResponse {
    HttpClient getHttpClient();
}
