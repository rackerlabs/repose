/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.core.services.httpclient.HttpClientContainer;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates apache http clients with basic auth
 */
public class ServiceClient {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceClient.class);
    private String connectionPoolId;

    private HttpClientService httpClientService;

    public ServiceClient(String connectionPoolId, HttpClientService httpClientService) {
        this.connectionPoolId = connectionPoolId;
        this.httpClientService = httpClientService;
    }

    private HttpClient getClientWithBasicAuth() {
        HttpClientContainer clientResponse = null;
        try {
            clientResponse = httpClientService.getClient(connectionPoolId);
            return clientResponse.getHttpClient();
        } finally {
            if (clientResponse != null) {
                httpClientService.releaseClient(clientResponse);
            }
        }
    }

    private void setHeaders(HttpRequestBase base, Map<String, String> headers) {
        final Set<Map.Entry<String, String>> entries = headers.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            base.addHeader(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("squid:S2093")
    private ServiceClientResponse execute(HttpRequestBase base, String... queryParameters) {
        // I'm not exactly sure why this rule is triggering on this since there is nothing be created outside of the try.
        // So it is safe to suppress warning squid:S2093
        try {
            HttpClient client = getClientWithBasicAuth();
            for (int index = 0; index < queryParameters.length; index = index + 2) {
                client.getParams().setParameter(queryParameters[index], queryParameters[index + 1]);
            }
            HttpResponse httpResponse = client.execute(base);
            HttpEntity entity = httpResponse.getEntity();
            InputStream stream = new ByteArrayInputStream(new byte[0]);
            if (entity != null) {
                stream = new ByteArrayInputStream(RawInputStreamReader.instance().readFully(entity.getContent()));
                EntityUtils.consume(entity);
            }
            return new ServiceClientResponse(httpResponse.getStatusLine().getStatusCode(), httpResponse.getAllHeaders(), stream);
        } catch (IOException ex) {
            LOG.error("Error executing request", ex);
        } finally {
            base.releaseConnection();
        }
        return new ServiceClientResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
    }

    public ServiceClientResponse post(String uri, String body, MediaType contentMediaType) {
        return post(uri, new HashMap<>(), body, contentMediaType);
    }

    public ServiceClientResponse post(String uri, Map<String, String> headers, String body, MediaType contentMediaType) {
        HttpPost post = new HttpPost(uri);
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.putAll(headers);
        String localContentType = contentMediaType.getType() + "/" + contentMediaType.getSubtype();
        requestHeaders.put(CommonHttpHeader.CONTENT_TYPE.toString(), localContentType);

        // TODO: Remove setting the accept type to XML by default
        if (!requestHeaders.containsKey(CommonHttpHeader.ACCEPT.toString())) {
            requestHeaders.put(CommonHttpHeader.ACCEPT.toString(), MediaType.APPLICATION_XML);
        }
        setHeaders(post, requestHeaders);
        if (body != null && !body.isEmpty()) {
            post.setEntity(new InputStreamEntity(new ByteArrayInputStream(body.getBytes()), body.length()));
        }
        return execute(post);
    }

    public ServiceClientResponse get(String uri, Map<String, String> headers, String... queryParameters) {
        URI uriBuilt;
        HttpGet httpget = new HttpGet(uri);
        if (queryParameters != null) {
            if (queryParameters.length % 2 != 0) {
                throw new IllegalArgumentException("Query parameters must be in pairs.");
            }
            try {
                URIBuilder builder = new URIBuilder(uri);
                for (int index = 0; index < queryParameters.length; index = index + 2) {
                    builder.setParameter(queryParameters[index], queryParameters[index + 1]);
                }
                uriBuilt = builder.build();
                httpget = new HttpGet(uriBuilt);
            } catch (URISyntaxException e) {
                LOG.error("Error building request URI", e);
                return new ServiceClientResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
            }
        }
        setHeaders(httpget, headers);
        return execute(httpget);
    }
}
